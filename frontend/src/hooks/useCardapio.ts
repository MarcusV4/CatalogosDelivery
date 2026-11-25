import { useQuery } from '@tanstack/react-query';
import { chavesCardapio, listarCardapio } from '../services/cardapio';

/** Produtos do cardápio do restaurante. Sem toast: quem usa mostra o erro na tela. */
export function useCardapio(restauranteId: number) {
  return useQuery({
    queryKey: chavesCardapio.doRestaurante(restauranteId),
    queryFn: ({ signal }) => listarCardapio(restauranteId, signal),
    meta: { silenciarErro: true },
  });
}
