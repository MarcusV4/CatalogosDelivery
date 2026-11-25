import type { Restaurante } from '../../services/restaurantes';
import { situacaoDoRestaurante } from '../../utils/situacao';
import './styles.css';

interface SeloSituacaoProps {
  restaurante: Pick<Restaurante, 'abertoAgora' | 'statusRestaurante'>;
}

/** Selo "Aberto agora" / "Fechado agora" / "Indisponível" com o texto visível (não só a cor). */
export function SeloSituacao({ restaurante }: SeloSituacaoProps) {
  const { tipo, rotulo } = situacaoDoRestaurante(restaurante);
  return <span className={`selo selo--${tipo}`}>{rotulo}</span>;
}
