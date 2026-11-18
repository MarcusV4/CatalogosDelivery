/** Tipos do serviço de Cardápio. As chamadas entram nas issues das telas de cardápio. */

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

/** Produto de um cardápio ativo (`GET /api/produtos/{id}`). Preço em centavos. */
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
