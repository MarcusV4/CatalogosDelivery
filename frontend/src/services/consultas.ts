import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { ApiError } from './http';

/** `meta: { silenciarErro: true }` desliga o toast automático quando a tela trata o erro. */
export type MetaConsulta = Record<string, unknown> & { silenciarErro?: boolean };

declare module '@tanstack/react-query' {
  interface Register {
    queryMeta: MetaConsulta;
    mutationMeta: MetaConsulta;
  }
}

/** Dados ficam "frescos" por 30 s: voltar a uma tela não refaz a chamada na hora. */
const TEMPO_DADOS_FRESCOS_MS = 30_000;
/** Até 2 novas tentativas para falhas de rede e 5xx; erros 4xx não se resolvem repetindo. */
const MAXIMO_DE_NOVAS_TENTATIVAS = 2;
const MENSAGEM_ERRO_INESPERADO = 'Ocorreu um erro inesperado. Tente novamente.';

export function mensagemDoErro(erro: unknown): string {
  return erro instanceof ApiError ? erro.message : MENSAGEM_ERRO_INESPERADO;
}

export function deveTentarDeNovo(falhas: number, erro: unknown): boolean {
  if (erro instanceof ApiError && erro.ehErroDoCliente) {
    return false;
  }
  return falhas < MAXIMO_DE_NOVAS_TENTATIVAS;
}

/** QueryClient da aplicação: erros de consultas e mutações vão para o toast via `notificarErro`. */
export function criarQueryClient(notificarErro: (mensagem: string) => void): QueryClient {
  const avisar = (erro: unknown, meta: MetaConsulta | undefined) => {
    if (!meta?.silenciarErro) {
      notificarErro(mensagemDoErro(erro));
    }
  };

  return new QueryClient({
    queryCache: new QueryCache({ onError: (erro, consulta) => avisar(erro, consulta.meta) }),
    mutationCache: new MutationCache({
      onError: (erro, _variaveis, _resultado, mutacao) => avisar(erro, mutacao.meta),
    }),
    defaultOptions: {
      queries: {
        staleTime: TEMPO_DADOS_FRESCOS_MS,
        retry: deveTentarDeNovo,
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  });
}
