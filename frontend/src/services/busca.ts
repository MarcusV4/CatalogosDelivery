import { requisitar } from './http';
import type { CategoriaRestaurante, StatusRestaurante } from './restaurantes';

/** Página devolvida pelas rotas `/api/busca/*` (`pagina` começa em 0). */
export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}

/** Restaurante de `GET /api/busca/restaurantes` (sem endereço completo, telefone nem dono). */
export interface RestauranteBusca {
  restauranteId: number;
  restauranteNome: string;
  restauranteDesc: string | null;
  cidade: string | null;
  estado: string | null;
  urlImagem: string | null;
  statusRestaurante: StatusRestaurante;
  categoriaRestaurante: CategoriaRestaurante;
  /** Em centavos. */
  taxaEntrega: number;
  /** Em centavos, sem a taxa de entrega; `null` quando não há mínimo. */
  pedidoMinimo: number | null;
  tempoEstimadoMin: number | null;
  tempoEstimadoMax: number | null;
  abertoAgora: boolean;
}

/** Item de `GET /api/busca/itens`. Os campos `restaurante*` (exceto o id) ficam nulos se Restaurantes cair. */
export interface ItemBusca {
  id: number;
  nome: string;
  descricao: string | null;
  /** Em centavos. */
  preco: number;
  disponivel: boolean;
  categoria: string;
  urlImagem: string | null;
  restauranteId: number;
  restauranteNome: string | null;
  restauranteStatus: StatusRestaurante | null;
  restauranteAbertoAgora: boolean | null;
}

interface Paginacao {
  /** Página da API, começando em 0. */
  page: number;
  /** 1 a 100. */
  size: number;
}

/** Parâmetros de `GET /api/busca/restaurantes`; ausente = não filtra. */
export interface ParametrosBuscaRestaurantes extends Paginacao {
  nome?: string;
  categoria?: CategoriaRestaurante;
  /** Só `true`: `false` significaria "só os fechados agora". */
  abertoAgora?: true;
}

/** Parâmetros de `GET /api/busca/itens`; ausente = não filtra. Preços em centavos, limites incluídos. */
export interface ParametrosBuscaItens extends Paginacao {
  nome?: string;
  categoriaRestaurante?: CategoriaRestaurante;
  abertoAgora?: true;
  precoMin?: number;
  precoMax?: number;
}

export const chavesBusca = {
  todos: ['busca'] as const,
  restaurantes: (parametros: ParametrosBuscaRestaurantes) =>
    [...chavesBusca.todos, 'restaurantes', parametros] as const,
  itens: (parametros: ParametrosBuscaItens) => [...chavesBusca.todos, 'itens', parametros] as const,
};

/** Query string só com os parâmetros definidos, na ordem em que aparecem no objeto. */
export function montarQuery(parametros: object): string {
  const query = new URLSearchParams();
  for (const [nome, valor] of Object.entries(parametros)) {
    if (valor !== undefined) {
      query.set(nome, String(valor));
    }
  }
  return query.toString();
}

export function buscarRestaurantes(
  parametros: ParametrosBuscaRestaurantes,
  signal?: AbortSignal,
): Promise<Pagina<RestauranteBusca>> {
  return requisitar<Pagina<RestauranteBusca>>(
    'busca',
    `/api/busca/restaurantes?${montarQuery(parametros)}`,
    { signal },
  );
}

export function buscarItens(
  parametros: ParametrosBuscaItens,
  signal?: AbortSignal,
): Promise<Pagina<ItemBusca>> {
  return requisitar<Pagina<ItemBusca>>('busca', `/api/busca/itens?${montarQuery(parametros)}`, {
    signal,
  });
}
