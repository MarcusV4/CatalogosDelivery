import type { ItemBusca } from '../services/busca';
import { ApiError } from '../services/http';

export interface Mensagem {
  titulo: string;
  descricao: string;
}

export const MENSAGEM_BUSCA_INDISPONIVEL: Mensagem = {
  titulo: 'A busca está indisponível no momento.',
  descricao: 'Tente novamente em instantes.',
};

export const MENSAGEM_ERRO_BUSCA: Mensagem = {
  titulo: 'Não foi possível fazer a busca.',
  descricao: 'Confira os filtros ou tente de novo.',
};

export const AVISO_DADOS_DO_RESTAURANTE =
  'Alguns dados dos restaurantes não puderam ser carregados agora.';

/**
 * Mensagem amigável, sem detalhes técnicos. Busca fora do ar (sem resposta) e 5xx (inclusive
 * 503 quando Restaurantes ou Cardápio caem) contam como indisponibilidade.
 */
export function mensagemDeErroDaBusca(erro: unknown): Mensagem {
  const indisponivel = !(erro instanceof ApiError) || erro.ehIndisponivel;
  return indisponivel ? MENSAGEM_BUSCA_INDISPONIVEL : MENSAGEM_ERRO_BUSCA;
}

/** Lista vazia: com filtros, sugere mudar ou limpar; sem filtros, não há o que mostrar. */
export function mensagemDeVazio(singular: string, comFiltros: boolean): Mensagem {
  return comFiltros
    ? {
        titulo: `Nenhum ${singular} encontrado com esses filtros.`,
        descricao: 'Tente outro nome ou categoria, ou limpe os filtros.',
      }
    : {
        titulo: `Nenhum ${singular} disponível no momento.`,
        descricao: 'Volte mais tarde para ver as novidades.',
      };
}

/** A Busca devolve os itens sem os dados do restaurante quando o serviço de Restaurantes cai. */
export function faltamDadosDoRestaurante(itens: readonly ItemBusca[]): boolean {
  return itens.some((item) => item.restauranteNome === null);
}
