import { useState, type FormEvent } from 'react';
import { Botao } from '../../components/Botao';
import { type DiaSemana, type HorarioFuncionamento } from '../../services/restaurantes';
import { ROTULOS_DIA_SEMANA } from '../../utils/rotulos';

interface FormularioHorariosProps {
  horarios: HorarioFuncionamento[];
  salvando: boolean;
  erro?: string | null;
  aoSalvar: (horarios: HorarioFuncionamento[]) => void;
}

const DIAS: DiaSemana[] = ['SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO', 'DOMINGO'];

type FaixaDia = { aberto: boolean; abertura: string; fechamento: string };
type Semana = Record<DiaSemana, FaixaDia>;

function criarSemana(horarios: HorarioFuncionamento[]): Semana {
  const semana = Object.fromEntries(
    DIAS.map((dia) => [dia, { aberto: false, abertura: '09:00', fechamento: '18:00' }]),
  ) as Semana;
  for (const horario of horarios) {
    semana[horario.diaSemana] = {
      aberto: true,
      abertura: horario.horaAbertura.slice(0, 5),
      fechamento: horario.horaFechamento.slice(0, 5),
    };
  }
  return semana;
}

/** Edição semanal completa; dias desmarcados são enviados como fechados por omissão. */
export function FormularioHorarios({
  horarios,
  salvando,
  erro,
  aoSalvar,
}: FormularioHorariosProps) {
  const [semana, setSemana] = useState(() => criarSemana(horarios));
  const [erroLocal, setErroLocal] = useState<string | null>(null);

  const alterar = (dia: DiaSemana, alteracoes: Partial<FaixaDia>) =>
    setSemana((atual) => ({ ...atual, [dia]: { ...atual[dia], ...alteracoes } }));

  const enviar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    setErroLocal(null);
    const horariosAtivos: HorarioFuncionamento[] = [];
    for (const dia of DIAS) {
      const faixa = semana[dia];
      if (!faixa.aberto) continue;
      if (!faixa.abertura || !faixa.fechamento || faixa.abertura >= faixa.fechamento) {
        setErroLocal(
          `Confira os horários de ${ROTULOS_DIA_SEMANA[dia].toLowerCase()}; o fechamento deve ser depois da abertura e no mesmo dia.`,
        );
        return;
      }
      horariosAtivos.push({
        diaSemana: dia,
        horaAbertura: `${faixa.abertura}:00`,
        horaFechamento: `${faixa.fechamento}:00`,
      });
    }
    aoSalvar(horariosAtivos);
  };

  return (
    <form className="painel-formulario painel-restaurantes__horarios" onSubmit={enviar} noValidate>
      <div>
        <h2>Horários de funcionamento</h2>
        <p className="mudo">
          Marque os dias abertos e informe os horários. Dias desmarcados ficam fechados.
        </p>
      </div>
      <div className="painel-restaurantes__dias">
        {DIAS.map((dia) => {
          const faixa = semana[dia];
          return (
            <div className="painel-restaurantes__dia" key={dia}>
              <label className="painel-restaurantes__dia-nome" htmlFor={`dia-${dia}`}>
                <input
                  id={`dia-${dia}`}
                  type="checkbox"
                  checked={faixa.aberto}
                  disabled={salvando}
                  onChange={(evento) => alterar(dia, { aberto: evento.target.checked })}
                />
                {ROTULOS_DIA_SEMANA[dia]}
              </label>
              {faixa.aberto ? (
                <div className="painel-restaurantes__faixa">
                  <label htmlFor={`abre-${dia}`}>Abre</label>
                  <input
                    id={`abre-${dia}`}
                    type="time"
                    value={faixa.abertura}
                    disabled={salvando}
                    onChange={(evento) => alterar(dia, { abertura: evento.target.value })}
                  />
                  <label htmlFor={`fecha-${dia}`}>Fecha</label>
                  <input
                    id={`fecha-${dia}`}
                    type="time"
                    value={faixa.fechamento}
                    disabled={salvando}
                    onChange={(evento) => alterar(dia, { fechamento: evento.target.value })}
                  />
                </div>
              ) : (
                <span className="mudo">Fechado</span>
              )}
            </div>
          );
        })}
      </div>
      {erroLocal ? (
        <p className="painel-restaurantes__erro" role="alert">
          {erroLocal}
        </p>
      ) : null}
      {erro ? (
        <p className="painel-restaurantes__erro" role="alert">
          {erro}
        </p>
      ) : null}
      <div className="painel-formulario__acoes">
        <Botao type="submit" disabled={salvando}>
          {salvando ? 'Salvando horários…' : 'Salvar horários'}
        </Botao>
      </div>
    </form>
  );
}
