import type { Restaurante } from '../services/restaurantes';
import { ROTULOS_STATUS_RESTAURANTE } from './rotulos';

export type TipoSituacao = 'aberto' | 'fechado' | 'indisponivel';

export interface Situacao {
  tipo: TipoSituacao;
  rotulo: string;
}

/** Situação mostrada ao cliente: `abertoAgora` já considera status e horário (calculado no back). */
export function situacaoDoRestaurante({
  abertoAgora,
  statusRestaurante,
}: Pick<Restaurante, 'abertoAgora' | 'statusRestaurante'>): Situacao {
  if (abertoAgora) {
    return { tipo: 'aberto', rotulo: 'Aberto agora' };
  }
  if (statusRestaurante === 'INDISPONIVEL') {
    return { tipo: 'indisponivel', rotulo: ROTULOS_STATUS_RESTAURANTE.INDISPONIVEL };
  }
  return { tipo: 'fechado', rotulo: 'Fechado agora' };
}
