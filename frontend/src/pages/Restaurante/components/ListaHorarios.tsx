import { useState } from 'react';
import type { HorarioFuncionamento } from '../../../services/restaurantes';
import {
  diaDaSemanaEm,
  montarSemana,
  resumoDeHoje,
  temHorarios,
  textoIntervalo,
} from '../../../utils/horarios';

interface ListaHorariosProps {
  horarios: readonly HorarioFuncionamento[];
}

/** Resumo de hoje, que expande para a semana inteira (dia atual em destaque). */
export function ListaHorarios({ horarios }: ListaHorariosProps) {
  // O dia é lido uma vez ao montar: a página não fica aberta de um dia para o outro.
  const [hoje] = useState(() => diaDaSemanaEm(new Date()));
  const semana = montarSemana(horarios, hoje);

  if (!temHorarios(semana)) {
    return <span className="mudo">Sem horários cadastrados.</span>;
  }

  return (
    <details className="horarios">
      <summary>{resumoDeHoje(semana)}</summary>
      <ul className="horas">
        {semana.map((linha) => (
          <li key={linha.dia} className={linha.hoje ? 'horas__hoje' : undefined}>
            <span>
              {linha.rotulo}
              {linha.hoje ? <span className="sr"> (hoje)</span> : null}
            </span>
            <span>{textoIntervalo(linha.intervalo)}</span>
          </li>
        ))}
      </ul>
    </details>
  );
}
