import { useQuery } from '@tanstack/react-query';
import { chavesRestaurantes, listarHorarios } from '../services/restaurantes';

/** Horários de funcionamento do restaurante. Sem toast: quem usa mostra o erro na tela. */
export function useHorarios(restauranteId: number) {
  return useQuery({
    queryKey: chavesRestaurantes.horarios(restauranteId),
    queryFn: ({ signal }) => listarHorarios(restauranteId, signal),
    meta: { silenciarErro: true },
  });
}
