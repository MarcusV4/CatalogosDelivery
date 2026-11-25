import { keepPreviousData, useQuery } from '@tanstack/react-query';
import {
  buscarRestaurantes,
  chavesBusca,
  type ParametrosBuscaRestaurantes,
} from '../services/busca';

/**
 * Busca de restaurantes. Mantém a página anterior visível enquanto a nova carrega
 * (`isPlaceholderData`). Sem toast: quem usa mostra o erro na tela.
 */
export function useBuscaRestaurantes(parametros: ParametrosBuscaRestaurantes) {
  return useQuery({
    queryKey: chavesBusca.restaurantes(parametros),
    queryFn: ({ signal }) => buscarRestaurantes(parametros, signal),
    placeholderData: keepPreviousData,
    meta: { silenciarErro: true },
  });
}
