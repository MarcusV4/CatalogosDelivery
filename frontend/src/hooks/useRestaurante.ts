import { useQuery } from '@tanstack/react-query';
import { buscarRestaurante, chavesRestaurantes } from '../services/restaurantes';

/** Restaurante pelo id. Sem toast: quem usa mostra o erro (inclusive o 404) na tela. */
export function useRestaurante(id: number) {
  return useQuery({
    queryKey: chavesRestaurantes.detalhe(id),
    queryFn: ({ signal }) => buscarRestaurante(id, signal),
    meta: { silenciarErro: true },
  });
}
