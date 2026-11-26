/** Papel lido da claim `role`, normalizado: maiúsculas e sem o prefixo `ROLE_` do Spring. */
export type Papel = string;

/**
 * Papéis que abrem o painel do dono. O contrato da Identidade usa `LOJA` e o back-end do
 * Catálogo (#31) usa `LOJISTA` por padrão; os dois valem até o contrato ser fechado.
 */
export const PAPEIS_DE_LOJA: readonly Papel[] = ['LOJA', 'LOJISTA'];

export interface ConfiguracaoOAuth {
  emissor: string;
  clientId: string;
  redirectUri: string;
  escopos: string;
}

export interface MetadadosOAuth {
  issuer: string;
  authorization_endpoint: string;
  token_endpoint: string;
  revocation_endpoint?: string;
}

export interface SessaoOAuth {
  accessToken: string;
  refreshToken: string;
  tipo: Papel;
  usuarioId: string;
  expiraEm: number;
}

type AmbienteOAuth = Pick<
  ImportMetaEnv,
  | 'VITE_API_IDENTIDADE_URL'
  | 'VITE_IDENTIDADE_CLIENT_ID'
  | 'VITE_IDENTIDADE_REDIRECT_URI'
  | 'VITE_IDENTIDADE_SCOPES'
>;

interface RespostaTokens {
  access_token: string;
  refresh_token?: string;
  token_type: string;
  expires_in: number;
  error?: string;
  error_description?: string;
  mensagem?: string;
}

interface ClaimsAcesso {
  sub: string;
  role: Papel;
  exp: number;
}

const TEMPO_LIMITE_MS = 10_000;
let sessaoAtual: SessaoOAuth | null = null;

/** Sessão em memória: tokens de usuário não são persistidos no armazenamento do navegador. */
export function lerSessaoAtual(): SessaoOAuth | null {
  return sessaoAtual;
}

export function definirSessaoAtual(sessao: SessaoOAuth): void {
  sessaoAtual = sessao;
}

export function limparSessaoAtual(): void {
  sessaoAtual = null;
}

/** Mesma normalização do back-end: ignora maiúsculas e o prefixo `ROLE_`. */
export function normalizarPapel(role: string): Papel {
  const maiusculo = role.trim().toUpperCase();
  return maiusculo.startsWith('ROLE_') ? maiusculo.slice('ROLE_'.length) : maiusculo;
}

/** A sessão é de uma conta de loja (dona de restaurantes)? Só controla a interface. */
export function ehContaDeLoja(sessao: SessaoOAuth | null): boolean {
  return sessao !== null && PAPEIS_DE_LOJA.includes(sessao.tipo);
}

/** Lê a configuração do cliente público de navegador; segredo nunca é aceito aqui. */
export function configuracaoOAuth(env: AmbienteOAuth = import.meta.env): ConfiguracaoOAuth {
  const emissor = env.VITE_API_IDENTIDADE_URL?.trim().replace(/\/+$/, '') ?? '';
  const clientId = env.VITE_IDENTIDADE_CLIENT_ID?.trim() ?? '';
  // `||`: variável presente mas vazia no .env também usa o callback padrão.
  const redirectUri =
    env.VITE_IDENTIDADE_REDIRECT_URI?.trim() || `${window.location.origin}/oauth/callback`;
  const escopos = env.VITE_IDENTIDADE_SCOPES?.trim() ?? '';

  if (!emissor || !clientId || !redirectUri) {
    throw new Error(
      'Configure VITE_API_IDENTIDADE_URL, VITE_IDENTIDADE_CLIENT_ID e VITE_IDENTIDADE_REDIRECT_URI.',
    );
  }
  return { emissor, clientId, redirectUri, escopos };
}

/** Obtém os endpoints do Authorization Server por descoberta OAuth. */
export async function obterMetadadosOAuth(
  config: ConfiguracaoOAuth,
  requisitar: typeof fetch = fetch,
): Promise<MetadadosOAuth> {
  let resposta: Response;
  try {
    resposta = await requisitar(`${config.emissor}/.well-known/oauth-authorization-server`, {
      headers: { Accept: 'application/json' },
      signal: AbortSignal.timeout(TEMPO_LIMITE_MS),
    });
  } catch {
    throw new Error('Não foi possível conectar ao serviço de autenticação.');
  }
  if (!resposta.ok) {
    throw new Error('O serviço de autenticação está indisponível no momento. Tente novamente.');
  }
  const metadados: unknown = await resposta.json().catch(() => undefined);
  if (!ehMetadadosOAuth(metadados) || normalizarEmissor(metadados.issuer) !== config.emissor) {
    throw new Error('O serviço de autenticação retornou metadados inválidos.');
  }
  validarEndpoint(metadados.authorization_endpoint);
  validarEndpoint(metadados.token_endpoint);
  if (metadados.revocation_endpoint) validarEndpoint(metadados.revocation_endpoint);
  return metadados;
}

/** Monta a URL do fluxo interativo sem enviar senha nem segredo do cliente ao navegador. */
export function criarUrlAutorizacao(
  endpoint: string,
  config: ConfiguracaoOAuth,
  state: string,
  challenge: string,
): string {
  const url = new URL(endpoint);
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('client_id', config.clientId);
  url.searchParams.set('redirect_uri', config.redirectUri);
  if (config.escopos) url.searchParams.set('scope', config.escopos);
  url.searchParams.set('state', state);
  url.searchParams.set('code_challenge', challenge);
  url.searchParams.set('code_challenge_method', 'S256');
  return url.toString();
}

/** Troca o código de autorização pelo par de tokens e extrai claims para a UI. */
export async function trocarCodigoPorTokens(
  config: ConfiguracaoOAuth,
  metadados: MetadadosOAuth,
  code: string,
  verifier: string,
  requisitar: typeof fetch = fetch,
): Promise<SessaoOAuth> {
  const parametros = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: config.redirectUri,
    client_id: config.clientId,
    code_verifier: verifier,
  });
  return enviarTokens(metadados.token_endpoint, parametros, requisitar);
}

/** Rotaciona o refresh token; uma resposta sem um novo refresh token invalida a sessão. */
export async function renovarTokens(
  config: ConfiguracaoOAuth,
  metadados: MetadadosOAuth,
  refreshToken: string,
  requisitar: typeof fetch = fetch,
): Promise<SessaoOAuth> {
  const parametros = new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: config.clientId,
  });
  return enviarTokens(metadados.token_endpoint, parametros, requisitar);
}

/** Revoga a família de sessão usando o refresh token (RFC 7009). */
export async function revogarRefreshToken(
  config: ConfiguracaoOAuth,
  metadados: MetadadosOAuth,
  refreshToken: string,
  requisitar: typeof fetch = fetch,
): Promise<void> {
  if (!metadados.revocation_endpoint) {
    throw new Error('O serviço de autenticação não publicou o endpoint de logout.');
  }
  const parametros = new URLSearchParams({
    token: refreshToken,
    token_type_hint: 'refresh_token',
    client_id: config.clientId,
  });
  let resposta: Response;
  try {
    resposta = await requisitar(metadados.revocation_endpoint, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded' },
      body: parametros.toString(),
      signal: AbortSignal.timeout(TEMPO_LIMITE_MS),
    });
  } catch {
    throw new Error('Não foi possível conectar ao serviço de autenticação para encerrar a sessão.');
  }
  if (!resposta.ok) {
    throw new Error('O serviço de autenticação não conseguiu encerrar a sessão. Tente novamente.');
  }
}

/** Decodifica claims do JWT apenas para apresentar o papel e o vencimento na interface. */
export function decodificarClaims(token: string): ClaimsAcesso {
  try {
    const parte = token.split('.')[1];
    if (!parte) throw new Error();
    const binario = atob(parte.replace(/-/g, '+').replace(/_/g, '/'));
    const bytes = Uint8Array.from(binario, (caractere) => caractere.charCodeAt(0));
    const claims: unknown = JSON.parse(new TextDecoder().decode(bytes));
    if (!ehClaimsAcesso(claims)) throw new Error();
    return { ...claims, role: normalizarPapel(claims.role) };
  } catch {
    throw new Error('O serviço de autenticação retornou um token inválido.');
  }
}

async function enviarTokens(
  endpoint: string,
  parametros: URLSearchParams,
  requisitar: typeof fetch,
): Promise<SessaoOAuth> {
  let resposta: Response;
  try {
    resposta = await requisitar(endpoint, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded' },
      body: parametros.toString(),
      signal: AbortSignal.timeout(TEMPO_LIMITE_MS),
    });
  } catch {
    throw new Error('Não foi possível conectar ao serviço de autenticação.');
  }
  const corpo: unknown = await resposta.json().catch(() => undefined);
  if (!resposta.ok) {
    const erro = corpo as Partial<RespostaTokens> | undefined;
    if (resposta.status >= 500) {
      throw new Error('O serviço de autenticação está indisponível no momento. Tente novamente.');
    }
    throw new Error(
      erro?.error_description ??
        erro?.mensagem ??
        'Não foi possível entrar. Confira os dados e tente novamente.',
    );
  }
  if (!ehRespostaTokens(corpo)) {
    throw new Error('O serviço de autenticação retornou uma resposta de sessão inválida.');
  }
  if (!corpo.refresh_token) {
    throw new Error(
      'O serviço de autenticação não retornou o refresh token necessário para manter a sessão.',
    );
  }
  const claims = decodificarClaims(corpo.access_token);
  return {
    accessToken: corpo.access_token,
    refreshToken: corpo.refresh_token,
    tipo: claims.role,
    usuarioId: claims.sub,
    expiraEm: Math.min(claims.exp * 1000, Date.now() + corpo.expires_in * 1000),
  };
}

function ehMetadadosOAuth(valor: unknown): valor is MetadadosOAuth {
  if (typeof valor !== 'object' || valor === null) return false;
  const metadados = valor as Partial<MetadadosOAuth>;
  return (
    typeof metadados.issuer === 'string' &&
    typeof metadados.authorization_endpoint === 'string' &&
    typeof metadados.token_endpoint === 'string' &&
    (metadados.revocation_endpoint === undefined ||
      typeof metadados.revocation_endpoint === 'string')
  );
}

function ehClaimsAcesso(valor: unknown): valor is ClaimsAcesso {
  if (typeof valor !== 'object' || valor === null) return false;
  const claims = valor as Partial<ClaimsAcesso>;
  return (
    typeof claims.sub === 'string' &&
    claims.sub.length > 0 &&
    // Papel desconhecido não impede o login: ele só não abre o painel do dono.
    typeof claims.role === 'string' &&
    claims.role.trim().length > 0 &&
    typeof claims.exp === 'number' &&
    Number.isFinite(claims.exp)
  );
}

function ehRespostaTokens(valor: unknown): valor is RespostaTokens {
  if (typeof valor !== 'object' || valor === null) return false;
  const tokens = valor as Partial<RespostaTokens>;
  return (
    typeof tokens.access_token === 'string' &&
    tokens.access_token.length > 0 &&
    typeof tokens.token_type === 'string' &&
    tokens.token_type.toLowerCase() === 'bearer' &&
    typeof tokens.expires_in === 'number' &&
    Number.isFinite(tokens.expires_in) &&
    tokens.expires_in > 0
  );
}

function normalizarEmissor(emissor: string): string {
  return emissor.replace(/\/+$/, '');
}

function validarEndpoint(endpoint: string): void {
  const url = new URL(endpoint);
  const seguroLocal = url.hostname === 'localhost' || url.hostname === '127.0.0.1';
  if (url.protocol !== 'https:' && !(seguroLocal && url.protocol === 'http:')) {
    throw new Error('O serviço de autenticação precisa usar HTTPS.');
  }
}
