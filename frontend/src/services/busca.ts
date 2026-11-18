/** Tipos do serviço de Busca. As chamadas entram na issue da tela "Buscar Prato". */

/** Página devolvida pelas rotas `/api/busca/*`. */
export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}

/** Item de `GET /api/busca/itens`. Os campos `restaurante*` (exceto o id) ficam nulos se Restaurantes cair. */
export interface ItemBusca {
  id: number;
  nome: string;
  descricao: string | null;
  preco: number;
  disponivel: boolean;
  categoria: string;
  urlImagem: string | null;
  restauranteId: number;
  restauranteNome: string | null;
  restauranteStatus: string | null;
  restauranteAbertoAgora: boolean | null;
}
