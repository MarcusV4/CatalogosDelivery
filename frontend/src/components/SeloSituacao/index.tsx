import type { Restaurante } from '../../services/restaurantes';
import type { Previsao } from '../../utils/horarios';
import { situacaoDoRestaurante } from '../../utils/situacao';
import './styles.css';

interface SeloSituacaoProps {
  restaurante: Pick<Restaurante, 'abertoAgora' | 'statusRestaurante'>;
  /** Próxima abertura ou fechamento, quando a tela tem os horários (página do restaurante). */
  previsao?: Previsao | null;
}

/** Selo "Aberto agora" / "Fechado agora" / "Indisponível" com o texto visível (não só a cor). */
export function SeloSituacao({ restaurante, previsao = null }: SeloSituacaoProps) {
  const { tipo, rotulo } = situacaoDoRestaurante(restaurante, previsao);
  return <span className={`selo selo--${tipo}`}>{rotulo}</span>;
}
