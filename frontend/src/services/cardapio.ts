import { requisitar } from './http';

export type CategoriaProduto =
  | 'LANCHE'
  | 'PRATO_PRINCIPAL'
  | 'PIZZA'
  | 'ENTRADA'
  | 'SOBREMESA'
  | 'BEBIDA'
  | 'VEGETARIANO'
  | 'SALGADO'
  | 'ORIENTAL';

/** Extra de um produto. Preço em centavos. */
export interface Adicional {
  id: number;
  nome: string;
  preco: number;
  disponivel: boolean;
}

/** Produto de um cardápio ativo (`GET /api/produtos/{id}` e cardápio do restaurante). Preço em centavos. */
export interface Produto {
  id: number;
  nome: string;
  descricao: string | null;
  preco: number;
  disponivel: boolean;
  categoria: CategoriaProduto;
  restauranteId: number;
  urlImagem: string | null;
  adicionais: Adicional[];
}

export const chavesCardapio = {
  doRestaurante: (restauranteId: number) => ['cardapio', 'restaurante', restauranteId] as const,
};

/**
 * Produtos do cardápio ativo de um restaurante, sem ordem garantida. Nunca responde 404:
 * restaurante inexistente, sem itens ou com cardápio excluído devolve `[]`.
 */
export function listarCardapio(restauranteId: number, signal?: AbortSignal): Promise<Produto[]> {
  return requisitar<Produto[]>('cardapio', `/api/restaurantes/${restauranteId}/cardapio`, {
    signal,
  });
}
