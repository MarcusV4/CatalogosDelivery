import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  configuracaoOAuth,
  criarUrlAutorizacao,
  decodificarClaims,
  ehContaDeLoja,
  obterMetadadosOAuth,
  revogarRefreshToken,
  trocarCodigoPorTokens,
  renovarTokens,
  definirSessaoAtual,
  lerSessaoAtual,
  limparSessaoAtual,
  type ConfiguracaoOAuth,
  type MetadadosOAuth,
  type SessaoOAuth,
} from './oauth';

const config: ConfiguracaoOAuth = {
  emissor: 'http://localhost:18001',
  clientId: 'catalogo-web',
  redirectUri: 'http://localhost:5173/oauth/callback',
  escopos: '',
};

const metadados: MetadadosOAuth = {
  issuer: config.emissor,
  authorization_endpoint: `${config.emissor}/v1/oauth/autorizar`,
  token_endpoint: `${config.emissor}/v1/oauth/tokens`,
  revocation_endpoint: `${config.emissor}/v1/oauth/revogar`,
};

function jwt(payload: Record<string, unknown>): string {
  const codificado = btoa(JSON.stringify(payload))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `header.${codificado}.signature`;
}

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('cliente OAuth', () => {
  it('lê issuer, client_id e redirect_uri do ambiente sem exigir segredo no navegador', () => {
    expect(
      configuracaoOAuth({
        VITE_API_IDENTIDADE_URL: 'http://localhost:18001/',
        VITE_IDENTIDADE_CLIENT_ID: 'front-publico',
        VITE_IDENTIDADE_REDIRECT_URI: 'http://localhost:5173/oauth/callback',
        VITE_IDENTIDADE_SCOPES: 'perfil:ler',
      }),
    ).toEqual({
      emissor: 'http://localhost:18001',
      clientId: 'front-publico',
      redirectUri: 'http://localhost:5173/oauth/callback',
      escopos: 'perfil:ler',
    });
  });

  it('explica quais configurações são obrigatórias se o client_id estiver ausente', () => {
    expect(() =>
      configuracaoOAuth({
        VITE_API_IDENTIDADE_URL: 'http://localhost:18001',
        VITE_IDENTIDADE_REDIRECT_URI: 'http://localhost:5173/oauth/callback',
      }),
    ).toThrow('VITE_API_IDENTIDADE_URL, VITE_IDENTIDADE_CLIENT_ID');
  });

  it('monta autorização Authorization Code com PKCE S256 sem segredo no navegador', () => {
    const url = new URL(
      criarUrlAutorizacao(
        metadados.authorization_endpoint,
        config,
        'estado-seguro',
        'desafio-pkce',
      ),
    );

    expect(url.origin + url.pathname).toBe(metadados.authorization_endpoint);
    expect(url.searchParams.get('response_type')).toBe('code');
    expect(url.searchParams.get('client_id')).toBe(config.clientId);
    expect(url.searchParams.get('redirect_uri')).toBe(config.redirectUri);
    expect(url.searchParams.get('state')).toBe('estado-seguro');
    expect(url.searchParams.get('code_challenge')).toBe('desafio-pkce');
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.has('client_secret')).toBe(false);
  });

  it('descobre endpoints no documento de metadados OAuth', async () => {
    const fetchFalso = vi.fn<typeof fetch>().mockResolvedValue(respostaJson(200, metadados));
    vi.stubGlobal('fetch', fetchFalso);

    await expect(obterMetadadosOAuth(config, fetchFalso)).resolves.toEqual(metadados);
    expect(fetchFalso).toHaveBeenCalledWith(
      `${config.emissor}/.well-known/oauth-authorization-server`,
      expect.objectContaining({ headers: { Accept: 'application/json' } }),
    );
  });

  it('troca código com verifier PKCE e cria sessão a partir das claims', async () => {
    const accessToken = jwt({ sub: 'usuario-123', role: 'LOJA', exp: 1_900_000_000 });
    const fetchFalso = vi.fn<typeof fetch>().mockResolvedValue(
      respostaJson(200, {
        access_token: accessToken,
        refresh_token: 'refresh-inicial',
        token_type: 'Bearer',
        expires_in: 1500,
      }),
    );
    vi.stubGlobal('fetch', fetchFalso);

    const sessao = await trocarCodigoPorTokens(
      config,
      metadados,
      'codigo-oauth',
      'verificador-pkce',
      fetchFalso,
    );

    expect(sessao).toMatchObject({
      accessToken,
      refreshToken: 'refresh-inicial',
      tipo: 'LOJA',
      usuarioId: 'usuario-123',
    });
    const [url, requisicao] = fetchFalso.mock.calls[0] ?? [];
    expect(url).toBe(metadados.token_endpoint);
    expect(requisicao?.headers).toMatchObject({
      'Content-Type': 'application/x-www-form-urlencoded',
    });
    const corpo = new URLSearchParams(String(requisicao?.body));
    expect(corpo.get('grant_type')).toBe('authorization_code');
    expect(corpo.get('code')).toBe('codigo-oauth');
    expect(corpo.get('code_verifier')).toBe('verificador-pkce');
    expect(corpo.get('client_id')).toBe(config.clientId);
    expect(corpo.has('client_secret')).toBe(false);
  });

  it('renova access token usando refresh token rotativo', async () => {
    const accessToken = jwt({ sub: 'usuario-123', role: 'LOJA', exp: 1_900_000_000 });
    const fetchFalso = vi.fn<typeof fetch>().mockResolvedValue(
      respostaJson(200, {
        access_token: accessToken,
        refresh_token: 'refresh-rotacionado',
        token_type: 'Bearer',
        expires_in: 1500,
      }),
    );
    vi.stubGlobal('fetch', fetchFalso);

    await expect(
      renovarTokens(config, metadados, 'refresh-anterior', fetchFalso),
    ).resolves.toMatchObject({ refreshToken: 'refresh-rotacionado', accessToken });
    const requisicao = fetchFalso.mock.calls[0]?.[1];
    const corpo = new URLSearchParams(String(requisicao?.body));
    expect(corpo.get('grant_type')).toBe('refresh_token');
    expect(corpo.get('refresh_token')).toBe('refresh-anterior');
  });

  it('revoga a sessão enviando o refresh token conforme RFC 7009', async () => {
    const fetchFalso = vi.fn<typeof fetch>().mockResolvedValue(new Response(null, { status: 200 }));
    vi.stubGlobal('fetch', fetchFalso);

    await revogarRefreshToken(config, metadados, 'refresh-atual', fetchFalso);

    const [url, requisicao] = fetchFalso.mock.calls[0] ?? [];
    expect(url).toBe(metadados.revocation_endpoint);
    const corpo = new URLSearchParams(String(requisicao?.body));
    expect(corpo.get('token')).toBe('refresh-atual');
    expect(corpo.get('token_type_hint')).toBe('refresh_token');
    expect(corpo.get('client_id')).toBe(config.clientId);
  });

  it('decodifica role e exp sem tratar a claim local como validação criptográfica', () => {
    expect(decodificarClaims(jwt({ sub: 'id-1', role: 'CLIENTE', exp: 1_900_000_000 }))).toEqual({
      sub: 'id-1',
      role: 'CLIENTE',
      exp: 1_900_000_000,
    });
  });

  it('normaliza o papel como o back-end: maiúsculas e sem o prefixo ROLE_', () => {
    expect(decodificarClaims(jwt({ sub: '57', role: 'ROLE_lojista', exp: 1_900_000_000 })).role).toBe(
      'LOJISTA',
    );
    expect(decodificarClaims(jwt({ sub: '57', role: ' loja ', exp: 1_900_000_000 })).role).toBe(
      'LOJA',
    );
  });

  it('aceita papel desconhecido no login, mas recusa token sem papel', () => {
    expect(decodificarClaims(jwt({ sub: '57', role: 'GERENTE', exp: 1_900_000_000 })).role).toBe(
      'GERENTE',
    );
    expect(() => decodificarClaims(jwt({ sub: '57', role: ' ', exp: 1_900_000_000 }))).toThrow(
      'token inválido',
    );
  });

  it('trata LOJA e LOJISTA como conta de loja, e os outros papéis não', () => {
    const sessao = (tipo: string): SessaoOAuth => ({
      accessToken: 'access',
      refreshToken: 'refresh',
      tipo,
      usuarioId: '57',
      expiraEm: 1_900_000_000_000,
    });

    expect(ehContaDeLoja(sessao('LOJA'))).toBe(true);
    expect(ehContaDeLoja(sessao('LOJISTA'))).toBe(true);
    expect(ehContaDeLoja(sessao('CLIENTE'))).toBe(false);
    expect(ehContaDeLoja(sessao('GERENTE'))).toBe(false);
    expect(ehContaDeLoja(null)).toBe(false);
  });

  it('usa o callback padrão quando VITE_IDENTIDADE_REDIRECT_URI está vazia', () => {
    vi.stubGlobal('window', { location: { origin: 'http://localhost:3000' } });

    expect(
      configuracaoOAuth({
        VITE_API_IDENTIDADE_URL: 'http://localhost:18001',
        VITE_IDENTIDADE_CLIENT_ID: 'front-publico',
        VITE_IDENTIDADE_REDIRECT_URI: '',
      }).redirectUri,
    ).toBe('http://localhost:3000/oauth/callback');
  });

  it('mantém a sessão em memória e permite removê-la', () => {
    const sessao = {
      accessToken: 'access',
      refreshToken: 'refresh',
      tipo: 'LOJA' as const,
      usuarioId: 'id-1',
      expiraEm: 1_900_000_000_000,
    };
    definirSessaoAtual(sessao);

    expect(lerSessaoAtual()).toBe(sessao);
    limparSessaoAtual();
    expect(lerSessaoAtual()).toBeNull();
  });

  it('rejeita resposta OAuth sem refresh token rotativo', async () => {
    const fetchFalso = vi.fn<typeof fetch>().mockResolvedValue(
      respostaJson(200, {
        access_token: jwt({ sub: 'id-1', role: 'LOJA', exp: 1_900_000_000 }),
        token_type: 'Bearer',
        expires_in: 1500,
      }),
    );

    await expect(renovarTokens(config, metadados, 'refresh-anterior', fetchFalso)).rejects.toThrow(
      'refresh token',
    );
  });

  it('transforma falha de rede do serviço OAuth em mensagem amigável', async () => {
    const fetchFalso = vi.fn<typeof fetch>().mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(obterMetadadosOAuth(config, fetchFalso)).rejects.toThrow(
      'Não foi possível conectar ao serviço de autenticação.',
    );
  });
});
