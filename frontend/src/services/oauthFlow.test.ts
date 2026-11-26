import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  concluirFluxoLogin,
  iniciarFluxoLogin,
  type ArmazenamentoTemporarioOAuth,
} from './oauthFlow';
import type { ConfiguracaoOAuth, MetadadosOAuth } from './oauth';

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

function armazenamentoFake(): ArmazenamentoTemporarioOAuth {
  const itens = new Map<string, string>();
  return {
    getItem: (chave) => itens.get(chave) ?? null,
    setItem: (chave, valor) => void itens.set(chave, valor),
    removeItem: (chave) => void itens.delete(chave),
  };
}

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function jwt(payload: Record<string, unknown>): string {
  const codificado = btoa(JSON.stringify(payload))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `header.${codificado}.signature`;
}

afterEach(() => vi.unstubAllGlobals());

describe('fluxo Authorization Code + PKCE', () => {
  it('gera state e verifier por transação e redireciona com challenge S256', async () => {
    const storage = armazenamentoFake();
    const requisitar = vi.fn<typeof fetch>().mockResolvedValue(respostaJson(200, metadados));
    const redirecionar = vi.fn();

    await iniciarFluxoLogin('/painel/restaurantes', {
      config,
      armazenamento: storage,
      requisitar,
      redirecionar,
      criptografia: globalThis.crypto,
    });

    const url = new URL(redirecionar.mock.calls[0]?.[0] ?? '');
    expect(url.origin + url.pathname).toBe(metadados.authorization_endpoint);
    expect(url.searchParams.get('state')).toBe(storage.getItem('oauth.state'));
    expect(storage.getItem('oauth.state')).toBeTruthy();
    expect(storage.getItem('oauth.verifier')).toMatch(/^[A-Za-z0-9_-]{43,}$/);
    expect(storage.getItem('oauth.returnTo')).toBe('/painel/restaurantes');
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.get('code_challenge')).not.toBe(storage.getItem('oauth.verifier'));
  });

  it('valida state, troca o código e devolve a rota solicitada', async () => {
    const storage = armazenamentoFake();
    storage.setItem('oauth.state', 'state-validado');
    storage.setItem('oauth.verifier', 'verifier-secreto');
    storage.setItem('oauth.returnTo', '/painel/restaurantes/1');
    const accessToken = jwt({ sub: 'user-1', role: 'LOJA', exp: 1_900_000_000 });
    const requisitar = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(respostaJson(200, metadados))
      .mockResolvedValueOnce(
        respostaJson(200, {
          access_token: accessToken,
          refresh_token: 'refresh-1',
          token_type: 'Bearer',
          expires_in: 1500,
        }),
      );

    const resultado = await concluirFluxoLogin('?code=auth-code&state=state-validado', {
      config,
      armazenamento: storage,
      requisitar,
    });

    expect(resultado.returnTo).toBe('/painel/restaurantes/1');
    expect(resultado.sessao).toMatchObject({ tipo: 'LOJA', usuarioId: 'user-1' });
    expect(storage.getItem('oauth.state')).toBeNull();
    expect(storage.getItem('oauth.verifier')).toBeNull();
    const corpo = new URLSearchParams(String(requisitar.mock.calls[1]?.[1]?.body));
    expect(corpo.get('code_verifier')).toBe('verifier-secreto');
  });

  it('não troca o código quando state não confere', async () => {
    const storage = armazenamentoFake();
    storage.setItem('oauth.state', 'state-original');
    storage.setItem('oauth.verifier', 'verifier');
    const requisitar = vi.fn<typeof fetch>();

    await expect(
      concluirFluxoLogin('?code=auth-code&state=state-forjado', {
        config,
        armazenamento: storage,
        requisitar,
      }),
    ).rejects.toThrow('state');
    expect(requisitar).not.toHaveBeenCalled();
  });

  it('não aceita callback com erro OAuth como login bem-sucedido', async () => {
    const storage = armazenamentoFake();
    storage.setItem('oauth.state', 'state-erro');
    storage.setItem('oauth.verifier', 'verifier');
    await expect(
      concluirFluxoLogin('?error=access_denied', {
        config,
        armazenamento: storage,
        requisitar: vi.fn<typeof fetch>(),
      }),
    ).rejects.toThrow('state inválido');
    const storageComStateValido = armazenamentoFake();
    storageComStateValido.setItem('oauth.state', 'state-erro');
    storageComStateValido.setItem('oauth.verifier', 'verifier');
    await expect(
      concluirFluxoLogin('?error=access_denied&state=state-erro', {
        config,
        armazenamento: storageComStateValido,
        requisitar: vi.fn<typeof fetch>(),
      }),
    ).rejects.toThrow('O login foi cancelado.');
    expect(storageComStateValido.getItem('oauth.verifier')).toBeNull();
  });

  it('não redireciona de volta para outro domínio', async () => {
    const storage = armazenamentoFake();
    storage.setItem('oauth.state', 'state-validado');
    storage.setItem('oauth.verifier', 'verifier');
    storage.setItem('oauth.returnTo', 'https://exemplo-malicioso.test');
    const requisitar = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(respostaJson(200, metadados))
      .mockResolvedValueOnce(
        respostaJson(200, {
          access_token: jwt({ sub: 'user-1', role: 'LOJA', exp: 1_900_000_000 }),
          refresh_token: 'refresh-1',
          token_type: 'Bearer',
          expires_in: 1500,
        }),
      );

    const resultado = await concluirFluxoLogin('?code=auth-code&state=state-validado', {
      config,
      armazenamento: storage,
      requisitar,
    });

    expect(resultado.returnTo).toBe('/');
  });
});
