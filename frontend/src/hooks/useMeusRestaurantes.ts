import { useQuery } from '@tanstack/react-query';
import { chavesRestaurantes, listarRestaurantes } from '../services/restaurantes';

/**
 * Restaurantes do dono logado: `ownerUserId` igual ao `sub` do token. A API ainda não
 * filtra por dono, então a lista vem completa e o filtro é feito aqui.
 */
export function useMeusRestaurantes(usuarioId: string) {
  const dono = Number(usuarioId);
  return useQuery({
    queryKey: [...chavesRestaurantes.todos, 'lista'] as const,
    queryFn: ({ signal }) => listarRestaurantes(signal),
    select: (restaurantes) =>
      Number.isSafeInteger(dono) ? restaurantes.filter((r) => r.ownerUserId === dono) : [],
    meta: { silenciarErro: true },
  });
}
