import { requisitar } from './http';

export type StatusRestaurante = 'ABERTO' | 'FECHADO' | 'INDISPONIVEL';

export type CategoriaRestaurante =
  | 'HAMBURGUERIA'
  | 'PIZZARIA'
  | 'COMIDA_BRASILEIRA'
  | 'COMIDA_REGIONAL'
  | 'COMIDA_JAPONESA'
  | 'COMIDA_CHINESA'
  | 'COMIDA_ITALIANA'
  | 'COMIDA_MEXICANA'
  | 'COMIDA_ARABE'
  | 'LANCHONETE'
  | 'MARMITARIA'
  | 'CHURRASCARIA'
  | 'COMIDA_SAUDAVEL'
  | 'COMIDA_VEGETARIANA_E_VEGANA'
  | 'PADARIA'
  | 'DOCERIA'
  | 'SORVETERIA'
  | 'ACAI'
  | 'OUTROS';

/** Resposta de `GET /api/restaurantes`. Valores monetários em centavos. */
export interface Restaurante {
  restauranteId: number;
  restauranteNome: string;
  restauranteDesc: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  estado: string | null;
  cep: string | null;
  restauranteTelefone: string | null;
  urlImagem: string | null;
  ownerUserId: number;
  statusRestaurante: StatusRestaurante;
  categoriaRestaurante: CategoriaRestaurante;
  taxaEntrega: number;
  tempoEstimadoMin: number | null;
  tempoEstimadoMax: number | null;
  /** `true` só com status ABERTO e dentro do horário de funcionamento (America/Recife). */
  abertoAgora: boolean;
}

export const chavesRestaurantes = {
  todos: ['restaurantes'] as const,
};

/** Restaurantes ativos (os desativados não aparecem). */
export function listarRestaurantes(signal?: AbortSignal): Promise<Restaurante[]> {
  return requisitar<Restaurante[]>('restaurantes', '/api/restaurantes', { signal });
}
