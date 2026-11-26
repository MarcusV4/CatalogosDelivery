import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNotificacao } from './useNotificacao';
import {
  atualizarRestaurante,
  atualizarStatusRestaurante,
  chavesRestaurantes,
  criarRestaurante,
  listarRestaurantesDoUsuario,
  type DadosRestaurante,
  type StatusRestaurante,
} from '../services/restaurantes';

/** Consultas e gravações usadas pelo painel de gerenciamento de restaurantes. */
export function useRestaurantesAdmin(usuarioId: number) {
  const cliente = useQueryClient();
  const { notificar } = useNotificacao();
  const chave = chavesRestaurantes.doUsuario(usuarioId);
  const restaurantes = useQuery({
    queryKey: chave,
    queryFn: ({ signal }) => listarRestaurantesDoUsuario(usuarioId, signal),
    meta: { silenciarErro: true },
  });

  const criar = useMutation({
    mutationFn: (dados: DadosRestaurante) => criarRestaurante(dados),
    meta: { silenciarErro: true },
    onSuccess: (restaurante) => {
      cliente.setQueryData<typeof restaurantes.data>(chave, (atuais) => [
        ...(atuais ?? []),
        restaurante,
      ]);
      notificar('Restaurante cadastrado.', 'sucesso');
    },
  });
  const atualizar = useMutation({
    mutationFn: ({ id, dados }: { id: number; dados: DadosRestaurante }) =>
      atualizarRestaurante(id, dados),
    meta: { silenciarErro: true },
    onSuccess: async (restaurante) => {
      cliente.setQueryData<typeof restaurantes.data>(chave, (atuais) =>
        atuais?.map((item) =>
          item.restauranteId === restaurante.restauranteId ? restaurante : item,
        ),
      );
      await cliente.invalidateQueries({
        queryKey: chavesRestaurantes.detalhe(restaurante.restauranteId),
      });
      notificar('Restaurante atualizado.', 'sucesso');
    },
  });
  const status = useMutation({
    mutationFn: ({ id, valor }: { id: number; valor: StatusRestaurante }) =>
      atualizarStatusRestaurante(id, valor),
    meta: { silenciarErro: true },
    onSuccess: (restaurante) => {
      // Mantém o item INDISPONIVEL visível na sessão atual, pois a listagem pública do serviço
      // ainda omite restaurantes desativados. A listagem completa do dono depende do back-end.
      cliente.setQueryData<typeof restaurantes.data>(chave, (atuais) => {
        const lista = atuais ?? [];
        return lista.some((item) => item.restauranteId === restaurante.restauranteId)
          ? lista.map((item) =>
              item.restauranteId === restaurante.restauranteId ? restaurante : item,
            )
          : [...lista, restaurante];
      });
      notificar('Status do restaurante atualizado.', 'sucesso');
    },
  });

  return { restaurantes, criar, atualizar, status };
}
