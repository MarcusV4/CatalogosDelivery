import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNotificacao } from './useNotificacao';
import {
  alterarDisponibilidadeProduto,
  atualizarProduto,
  chavesCardapio,
  criarProduto,
  excluirProduto,
  listarCardapio,
  listarCategorias,
  type ProdutoCadastro,
} from '../services/cardapio';

/** Consultas e gravações do painel; gravações atualizam o cardápio selecionado. */
export function useCardapioAdmin(restauranteId: number) {
  const cliente = useQueryClient();
  const { notificar } = useNotificacao();
  const cardapio = useQuery({
    queryKey: chavesCardapio.doRestaurante(restauranteId),
    queryFn: ({ signal }) => listarCardapio(restauranteId, signal),
    meta: { silenciarErro: true },
  });
  const categorias = useQuery({
    queryKey: chavesCardapio.categorias,
    queryFn: ({ signal }) => listarCategorias(signal),
    meta: { silenciarErro: true },
  });
  const atualizarLista = () =>
    cliente.invalidateQueries({ queryKey: chavesCardapio.doRestaurante(restauranteId) });

  const criar = useMutation({
    mutationFn: (produto: ProdutoCadastro) => criarProduto(restauranteId, produto),
    meta: { silenciarErro: true },
    onSuccess: async () => {
      await atualizarLista();
      notificar('Item cadastrado.', 'sucesso');
    },
  });
  const atualizar = useMutation({
    mutationFn: ({ id, produto }: { id: number; produto: ProdutoCadastro }) =>
      atualizarProduto(id, produto),
    meta: { silenciarErro: true },
    onSuccess: async () => {
      await atualizarLista();
      notificar('Item atualizado.', 'sucesso');
    },
  });
  const disponibilidade = useMutation({
    mutationFn: ({ id, disponivel }: { id: number; disponivel: boolean }) =>
      alterarDisponibilidadeProduto(id, disponivel),
    meta: { silenciarErro: true },
    onSuccess: async () => {
      await atualizarLista();
      notificar('Disponibilidade atualizada.', 'sucesso');
    },
  });
  const excluir = useMutation({
    mutationFn: (id: number) => excluirProduto(id),
    meta: { silenciarErro: true },
    onSuccess: async () => {
      await atualizarLista();
      notificar('Item excluído.', 'sucesso');
    },
  });

  return { cardapio, categorias, criar, atualizar, disponibilidade, excluir };
}
