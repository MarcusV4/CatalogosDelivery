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
  /** Conta dona: UUID do `sub` do token da Identidade. */
  ownerUserId: string;
  statusRestaurante: StatusRestaurante;
  categoriaRestaurante: CategoriaRestaurante;
  taxaEntrega: number;
  /** Valor mínimo dos itens do pedido, sem a taxa de entrega; `null` quando não há mínimo. */
  pedidoMinimo: number | null;
  tempoEstimadoMin: number | null;
  tempoEstimadoMax: number | null;
  /** `true` só com status ABERTO e dentro do horário de funcionamento (America/Recife). */
  abertoAgora: boolean;
}

/** Campos aceitos no cadastro e na atualização de um restaurante. */
export type DadosRestaurante = Omit<
  Restaurante,
  'restauranteId' | 'ownerUserId' | 'statusRestaurante' | 'abertoAgora'
>;

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
  doUsuario: (usuarioId: string) => [...chavesRestaurantes.todos, 'usuario', usuarioId] as const,
  detalhe: (id: number) => [...chavesRestaurantes.todos, id] as const,
  horarios: (id: number) => [...chavesRestaurantes.detalhe(id), 'horarios'] as const,
};

/** Lista os restaurantes ativos e filtra os do usuário autenticado para a interface do painel. */
export async function listarRestaurantesDoUsuario(
  usuarioId: string,
  signal?: AbortSignal,
): Promise<Restaurante[]> {
  const restaurantes = await requisitar<Restaurante[]>('restaurantes', '/api/restaurantes', {
    signal,
    exigirAutenticacao: true,
  });
  const conta = usuarioId.toLowerCase();
  return restaurantes.filter((restaurante) => restaurante.ownerUserId?.toLowerCase() === conta);
}

/** Cadastra restaurante; o serviço deve obter o proprietário do token, nunca do corpo. */
export function criarRestaurante(dados: DadosRestaurante): Promise<Restaurante> {
  return requisitar<Restaurante>('restaurantes', '/api/restaurantes', {
    metodo: 'POST',
    corpo: dados,
    exigirAutenticacao: true,
  });
}

/** Atualiza os dados cadastrais; a API valida a propriedade usando o token. */
export function atualizarRestaurante(id: number, dados: DadosRestaurante): Promise<Restaurante> {
  return requisitar<Restaurante>('restaurantes', `/api/restaurantes/${id}`, {
    metodo: 'PUT',
    corpo: dados,
    exigirAutenticacao: true,
  });
}

/** Altera o estado operacional do restaurante. */
export function atualizarStatusRestaurante(
  id: number,
  statusRestaurante: StatusRestaurante,
): Promise<Restaurante> {
  return requisitar<Restaurante>('restaurantes', `/api/restaurantes/${id}/status`, {
    metodo: 'PATCH',
    corpo: { statusRestaurante },
    exigirAutenticacao: true,
  });
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

/** Consulta horários no modo de gerenciamento, enviando o token da conta. */
export function listarHorariosGerenciamento(
  id: number,
  signal?: AbortSignal,
): Promise<HorarioFuncionamento[]> {
  return requisitar<HorarioFuncionamento[]>('restaurantes', `/api/restaurantes/${id}/horarios`, {
    signal,
    exigirAutenticacao: true,
  });
}

/** Substitui a semana inteira; dias omitidos permanecem fechados conforme o contrato da API. */
export function substituirHorarios(
  id: number,
  horarios: HorarioFuncionamento[],
): Promise<HorarioFuncionamento[]> {
  return requisitar<HorarioFuncionamento[]>('restaurantes', `/api/restaurantes/${id}/horarios`, {
    metodo: 'PUT',
    corpo: { horarios },
    exigirAutenticacao: true,
  });
}
