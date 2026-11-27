import type { Restaurante } from '../services/restaurantes';
import { textoPrevisao, type Previsao } from './horarios';
import { ROTULOS_STATUS_RESTAURANTE } from './rotulos';

export type TipoSituacao = 'aberto' | 'fechado' | 'indisponivel';

export interface Situacao {
  tipo: TipoSituacao;
  rotulo: string;
}

type EstadoRestaurante = Pick<Restaurante, 'abertoAgora' | 'statusRestaurante'>;

const AVISO_PEDIDOS = 'Você pode ver o cardápio, mas não pedir.';

/**
 * Só o restaurante com status ABERTO, fechado apenas pelo horário, tem previsão de abertura:
 * o FECHADO definido pelo dono não tem hora para acabar.
 */
function previsaoDeAbertura(
  { statusRestaurante }: EstadoRestaurante,
  previsao: Previsao | null,
): Previsao | null {
  return statusRestaurante === 'ABERTO' && previsao?.tipo === 'abre' ? previsao : null;
}

/**
 * Situação mostrada ao cliente: `abertoAgora` já considera status e horário (calculado no back).
 * Com a previsão dos horários, o rótulo segue o protótipo: "Aberto · fecha às 23:30" ou
 * "Fechado · abre amanhã às 11:00". Sem previsão, "Aberto agora" ou "Fechado agora".
 */
export function situacaoDoRestaurante(
  restaurante: EstadoRestaurante,
  previsao: Previsao | null = null,
): Situacao {
  if (restaurante.abertoAgora) {
    return {
      tipo: 'aberto',
      rotulo: previsao?.tipo === 'fecha' ? `Aberto · ${textoPrevisao(previsao)}` : 'Aberto agora',
    };
  }
  if (restaurante.statusRestaurante === 'INDISPONIVEL') {
    return { tipo: 'indisponivel', rotulo: ROTULOS_STATUS_RESTAURANTE.INDISPONIVEL };
  }
  const abertura = previsaoDeAbertura(restaurante, previsao);
  return {
    tipo: 'fechado',
    rotulo: abertura ? `Fechado · ${textoPrevisao(abertura)}` : 'Fechado agora',
  };
}

/** Aviso do protótipo para quando o restaurante não está aberto agora; `null` se está aberto. */
export function avisoDeFechado(
  restaurante: EstadoRestaurante,
  previsao: Previsao | null = null,
): string | null {
  if (restaurante.abertoAgora) {
    return null;
  }
  if (restaurante.statusRestaurante === 'INDISPONIVEL') {
    return `Este restaurante está temporariamente indisponível. ${AVISO_PEDIDOS}`;
  }
  const abertura = previsaoDeAbertura(restaurante, previsao);
  return abertura
    ? `Este restaurante está fechado agora (${textoPrevisao(abertura)}). ${AVISO_PEDIDOS}`
    : `Este restaurante está fechado agora. ${AVISO_PEDIDOS}`;
}
