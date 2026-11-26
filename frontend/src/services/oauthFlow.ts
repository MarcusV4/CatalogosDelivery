import {
  configuracaoOAuth,
  criarUrlAutorizacao,
  obterMetadadosOAuth,
  trocarCodigoPorTokens,
  type ConfiguracaoOAuth,
  type SessaoOAuth,
} from './oauth';

export interface ArmazenamentoTemporarioOAuth {
  getItem(chave: string): string | null;
  setItem(chave: string, valor: string): void;
  removeItem(chave: string): void;
}

interface DependenciasFluxoOAuth {
  config?: ConfiguracaoOAuth;
  armazenamento?: ArmazenamentoTemporarioOAuth;
  requisitar?: typeof fetch;
  redirecionar?: (url: string) => void;
  criptografia?: Crypto;
}

export interface ResultadoCallbackOAuth {
  sessao: SessaoOAuth;
  returnTo: string;
}

const CHAVE_STATE = 'oauth.state';
const CHAVE_VERIFIER = 'oauth.verifier';
const CHAVE_RETORNO = 'oauth.returnTo';

/** Persiste somente dados transitórios do PKCE no sessionStorage durante o redirect. */
export async function iniciarFluxoLogin(
  returnTo = '/',
  dependencias: DependenciasFluxoOAuth = {},
): Promise<void> {
  const config = dependencias.config ?? configuracaoOAuth();
  const armazenamento = dependencias.armazenamento ?? window.sessionStorage;
  const criptografia = dependencias.criptografia ?? window.crypto;
  const requisitar = dependencias.requisitar ?? fetch;
  const metadados = await obterMetadadosOAuth(config, requisitar);
  const state = gerarAleatorio(criptografia);
  const verifier = gerarAleatorio(criptografia);
  const challenge = base64Url(
    new Uint8Array(await criptografia.subtle.digest('SHA-256', new TextEncoder().encode(verifier))),
  );

  armazenamento.setItem(CHAVE_STATE, state);
  armazenamento.setItem(CHAVE_VERIFIER, verifier);
  armazenamento.setItem(CHAVE_RETORNO, caminhoInterno(returnTo));

  const destino = criarUrlAutorizacao(metadados.authorization_endpoint, config, state, challenge);
  (dependencias.redirecionar ?? ((url) => window.location.assign(url)))(destino);
}

/** Valida state, consome o código OAuth e limpa os dados transitórios mesmo em falhas. */
export async function concluirFluxoLogin(
  search: string,
  dependencias: DependenciasFluxoOAuth = {},
): Promise<ResultadoCallbackOAuth> {
  const parametros = new URLSearchParams(search);
  const armazenamento = dependencias.armazenamento ?? window.sessionStorage;
  const stateEsperado = armazenamento.getItem(CHAVE_STATE);
  const verifier = armazenamento.getItem(CHAVE_VERIFIER);
  const returnToSalvo = armazenamento.getItem(CHAVE_RETORNO);
  armazenamento.removeItem(CHAVE_STATE);
  armazenamento.removeItem(CHAVE_VERIFIER);
  armazenamento.removeItem(CHAVE_RETORNO);

  const code = parametros.get('code');
  const stateRecebido = parametros.get('state');
  if (!stateEsperado || !stateRecebido || stateRecebido !== stateEsperado) {
    throw new Error(
      'Não foi possível validar a resposta de login (state inválido). Tente entrar novamente.',
    );
  }
  const erroOAuth = parametros.get('error');
  if (erroOAuth) {
    throw new Error(
      erroOAuth === 'access_denied'
        ? 'O login foi cancelado.'
        : 'O serviço de autenticação não conseguiu concluir o login.',
    );
  }
  if (!code || !verifier) {
    throw new Error(
      'A resposta do serviço de autenticação está incompleta. Tente entrar novamente.',
    );
  }

  const config = dependencias.config ?? configuracaoOAuth();
  const metadados = await obterMetadadosOAuth(config, dependencias.requisitar ?? fetch);
  const sessao = await trocarCodigoPorTokens(
    config,
    metadados,
    code,
    verifier,
    dependencias.requisitar ?? fetch,
  );
  return { sessao, returnTo: caminhoInterno(returnToSalvo ?? '/') };
}

function gerarAleatorio(criptografia: Crypto): string {
  const bytes = criptografia.getRandomValues(new Uint8Array(32));
  return base64Url(bytes);
}

function base64Url(bytes: Uint8Array): string {
  let binario = '';
  for (const byte of bytes) binario += String.fromCharCode(byte);
  return btoa(binario).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
}

function caminhoInterno(caminho: string): string {
  return caminho.startsWith('/') && !caminho.startsWith('//') && !caminho.includes('\\')
    ? caminho
    : '/';
}
