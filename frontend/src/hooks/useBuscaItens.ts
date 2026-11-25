import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { buscarItens, chavesBusca, type ParametrosBuscaItens } from '../services/busca';

/**
 * Busca de pratos. Mantém a página anterior visível enquanto a nova carrega
 * (`isPlaceholderData`). Sem toast: quem usa mostra o erro na tela.
 */
export function useBuscaItens(parametros: ParametrosBuscaItens) {
  return useQuery({
    queryKey: chavesBusca.itens(parametros),
    queryFn: ({ signal }) => buscarItens(parametros, signal),
    placeholderData: keepPreviousData,
    meta: { silenciarErro: true },
  });
}
