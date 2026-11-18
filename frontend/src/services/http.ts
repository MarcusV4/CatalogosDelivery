import { NOMES_DOS_SERVICOS, origemDoServico, type Servico } from './config';

/** Status usado quando não houve resposta HTTP (rede, serviço fora do ar ou timeout). */
export const STATUS_SEM_RESPOSTA = 0;
export const TIMEOUT_PADRAO_MS = 10_000;

const STATUS_SEM_CONTEUDO = 204;
const PRIMEIRO_STATUS_DE_ERRO_DO_SERVIDOR = 500;
const PRIMEIRO_STATUS_DE_ERRO_DO_CLIENTE = 400;

/** Erro de uma chamada às APIs, com a mensagem pronta para mostrar ao usuário. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, mensagem: string) {
    super(mensagem);
    this.name = 'ApiError';
    this.status = status;
  }

  /** 4xx: repetir a mesma requisição não resolve. */
  get ehErroDoCliente(): boolean {
    return (
      this.status >= PRIMEIRO_STATUS_DE_ERRO_DO_CLIENTE &&
      this.status < PRIMEIRO_STATUS_DE_ERRO_DO_SERVIDOR
    );
  }
}

type MetodoHttp = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface OpcoesRequisicao {
  metodo?: MetodoHttp;
  corpo?: unknown;
  /** Sinal de cancelamento (o TanStack Query envia um para cada consulta). */
  signal?: AbortSignal;
  timeoutMs?: number;
}

interface ErroPadrao {
  erro: true;
  mensagem: string;
}

/**
 * Único ponto de chamada às APIs: monta a URL a partir da origem do serviço, envia e lê
 * JSON e converte qualquer falha em {@link ApiError}. Respostas 204 resolvem `undefined`.
 */
export async function requisitar<T>(
  servico: Servico,
  caminho: string,
  { metodo = 'GET', corpo, signal, timeoutMs = TIMEOUT_PADRAO_MS }: OpcoesRequisicao = {},
): Promise<T> {
  const url = `${origemDoServico(servico)}${caminho}`;
  const temCorpo = corpo !== undefined;
  const limiteDeTempo = AbortSignal.timeout(timeoutMs);

  let resposta: Response;
  try {
    resposta = await fetch(url, {
      method: metodo,
      headers: {
        Accept: 'application/json',
        ...(temCorpo ? { 'Content-Type': 'application/json' } : {}),
      },
      body: temCorpo ? JSON.stringify(corpo) : undefined,
      signal: signal ? AbortSignal.any([signal, limiteDeTempo]) : limiteDeTempo,
    });
  } catch (erro) {
    if (signal?.aborted) {
      throw erro; // cancelado por quem chamou: não é falha do serviço
    }
    throw erroSemResposta(servico, limiteDeTempo.aborted);
  }

  if (!resposta.ok) {
    throw await erroDaResposta(servico, resposta);
  }
  if (resposta.status === STATUS_SEM_CONTEUDO) {
    return undefined as T;
  }
  try {
    return (await resposta.json()) as T;
  } catch {
    throw new ApiError(
      resposta.status,
      `Resposta inesperada do serviço de ${NOMES_DOS_SERVICOS[servico]}.`,
    );
  }
}

function erroSemResposta(servico: Servico, estourouOTempo: boolean): ApiError {
  const nome = NOMES_DOS_SERVICOS[servico];
  const mensagem = estourouOTempo
    ? `O serviço de ${nome} demorou demais para responder. Tente novamente.`
    : `Não foi possível conectar ao serviço de ${nome}. Verifique sua conexão ou tente mais tarde.`;
  return new ApiError(STATUS_SEM_RESPOSTA, mensagem);
}

async function erroDaResposta(servico: Servico, resposta: Response): Promise<ApiError> {
  const corpo: unknown = await resposta.json().catch(() => undefined);
  if (ehErroPadrao(corpo)) {
    return new ApiError(resposta.status, corpo.mensagem);
  }
  const nome = NOMES_DOS_SERVICOS[servico];
  const mensagem =
    resposta.status >= PRIMEIRO_STATUS_DE_ERRO_DO_SERVIDOR
      ? `O serviço de ${nome} está indisponível no momento. Tente novamente em instantes.`
      : `Não foi possível concluir a requisição ao serviço de ${nome}.`;
  return new ApiError(resposta.status, mensagem);
}

/** Formato de erro de todos os serviços: `{"erro": true, "mensagem": "..."}`. */
function ehErroPadrao(corpo: unknown): corpo is ErroPadrao {
  return (
    typeof corpo === 'object' &&
    corpo !== null &&
    (corpo as Partial<ErroPadrao>).erro === true &&
    typeof (corpo as Partial<ErroPadrao>).mensagem === 'string'
  );
}
