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

/** Campos completos aceitos ao cadastrar ou editar um produto. Preço em centavos. */
export interface ProdutoCadastro {
  nome: string;
  descricao: string | null;
  preco: number;
  disponivel: boolean;
  urlImagem: string | null;
  categoria: CategoriaProduto;
}

export const chavesCardapio = {
  doRestaurante: (restauranteId: number) => ['cardapio', 'restaurante', restauranteId] as const,
  categorias: ['cardapio', 'categorias'] as const,
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

/** Categorias fixas aceitas pelo serviço de Cardápio. */
export function listarCategorias(signal?: AbortSignal): Promise<CategoriaProduto[]> {
  return requisitar<CategoriaProduto[]>('cardapio', '/api/categorias', { signal });
}

/** Cadastra um produto no cardápio ativo do restaurante selecionado. */
export function criarProduto(restauranteId: number, produto: ProdutoCadastro): Promise<Produto> {
  return requisitar<Produto>('cardapio', `/api/restaurantes/${restauranteId}/cardapio`, {
    metodo: 'POST',
    corpo: produto,
  });
}

/** Substitui todos os campos editáveis de um produto. */
export function atualizarProduto(id: number, produto: ProdutoCadastro): Promise<Produto> {
  return requisitar<Produto>('cardapio', `/api/produtos/${id}`, {
    metodo: 'PUT',
    corpo: produto,
  });
}

/** Altera somente a disponibilidade de um produto. */
export function alterarDisponibilidadeProduto(id: number, disponivel: boolean): Promise<Produto> {
  return requisitar<Produto>('cardapio', `/api/produtos/${id}/disponibilidade`, {
    metodo: 'PATCH',
    corpo: { disponivel },
  });
}

/** Exclui um produto; o endpoint responde sem corpo (204). */
export function excluirProduto(id: number): Promise<undefined> {
  return requisitar<undefined>('cardapio', `/api/produtos/${id}`, { metodo: 'DELETE' });
}
