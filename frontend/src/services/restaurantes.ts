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

/** Resposta de `GET /api/restaurantes/{id}`. Valores monetários em centavos. */
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

export type DiaSemana = 'DOMINGO' | 'SEGUNDA' | 'TERCA' | 'QUARTA' | 'QUINTA' | 'SEXTA' | 'SABADO';

/** Item de `GET /api/restaurantes/{id}/horarios`: um intervalo por dia, em `HH:mm:ss` (America/Recife). */
export interface HorarioFuncionamento {
  diaSemana: DiaSemana;
  horaAbertura: string;
  horaFechamento: string;
}

/** Chaves de cache. Invalidar `todos` invalida o detalhe e os horários de todos os restaurantes. */
export const chavesRestaurantes = {
  todos: ['restaurantes'] as const,
  detalhe: (id: number) => [...chavesRestaurantes.todos, id] as const,
  horarios: (id: number) => [...chavesRestaurantes.detalhe(id), 'horarios'] as const,
};

/** Restaurantes ativos (`GET /api/restaurantes`); os desativados (INDISPONIVEL) ficam de fora. */
export function listarRestaurantes(signal?: AbortSignal): Promise<Restaurante[]> {
  return requisitar<Restaurante[]>('restaurantes', '/api/restaurantes', { signal });
}

/** Um restaurante pelo id, inclusive desativado (INDISPONIVEL). Responde 404 se não existir. */
export function buscarRestaurante(id: number, signal?: AbortSignal): Promise<Restaurante> {
  return requisitar<Restaurante>('restaurantes', `/api/restaurantes/${id}`, { signal });
}

/** Horários da semana; dias ausentes estão fechados. A ordem da resposta não é a da semana. */
export function listarHorarios(id: number, signal?: AbortSignal): Promise<HorarioFuncionamento[]> {
  return requisitar<HorarioFuncionamento[]>('restaurantes', `/api/restaurantes/${id}/horarios`, {
    signal,
  });
}
