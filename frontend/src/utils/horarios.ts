import type { DiaSemana, HorarioFuncionamento } from '../services/restaurantes';
import { FUSO_HORARIO, formatarHorario } from './data';
import { ROTULOS_DIA_SEMANA } from './rotulos';

/** Ordem da semana exibida (igual ao protótipo e a `Date.getDay()`: domingo primeiro). */
export const DIAS_DA_SEMANA = [
  'DOMINGO',
  'SEGUNDA',
  'TERCA',
  'QUARTA',
  'QUINTA',
  'SEXTA',
  'SABADO',
] as const satisfies readonly DiaSemana[];

/** Horários já formatados em `HH:mm`. */
export interface Intervalo {
  abertura: string;
  fechamento: string;
}

export interface LinhaHorario {
  dia: DiaSemana;
  rotulo: string;
  /** `null` se o restaurante não abre nesse dia. */
  intervalo: Intervalo | null;
  hoje: boolean;
}

/** Dia da semana em inglês abreviado (`Intl`, independente do idioma do navegador) → enum do back. */
const DIA_POR_ABREVIACAO: Record<string, DiaSemana> = {
  Sun: 'DOMINGO',
  Mon: 'SEGUNDA',
  Tue: 'TERCA',
  Wed: 'QUARTA',
  Thu: 'QUINTA',
  Fri: 'SEXTA',
  Sat: 'SABADO',
};

const formatadorDiaDaSemana = new Intl.DateTimeFormat('en-US', {
  timeZone: FUSO_HORARIO,
  weekday: 'short',
});

/** Dia da semana do instante no fuso dos horários de funcionamento (America/Recife). */
export function diaDaSemanaEm(instante: Date): DiaSemana {
  const dia = DIA_POR_ABREVIACAO[formatadorDiaDaSemana.format(instante)];
  if (!dia) {
    throw new RangeError(`Data inválida: ${String(instante)}.`);
  }
  return dia;
}

/** `null` se algum horário vier fora do formato `HH:mm:ss`. */
function lerIntervalo({ horaAbertura, horaFechamento }: HorarioFuncionamento): Intervalo | null {
  try {
    return { abertura: formatarHorario(horaAbertura), fechamento: formatarHorario(horaFechamento) };
  } catch {
    return null;
  }
}

/** `"18:00 – 23:00"` ou `"Fechado"`. */
export function textoIntervalo(intervalo: Intervalo | null): string {
  return intervalo ? `${intervalo.abertura} – ${intervalo.fechamento}` : 'Fechado';
}

/**
 * Monta os 7 dias, de domingo a sábado, a partir da resposta do back (que não vem na ordem
 * da semana e omite os dias fechados). Entradas com horário inválido contam como dia sem horário.
 */
export function montarSemana(
  horarios: readonly HorarioFuncionamento[],
  hoje: DiaSemana,
): LinhaHorario[] {
  return DIAS_DA_SEMANA.map((dia) => {
    const horario = horarios.find((h) => h.diaSemana === dia);
    return {
      dia,
      rotulo: ROTULOS_DIA_SEMANA[dia],
      intervalo: horario ? lerIntervalo(horario) : null,
      hoje: dia === hoje,
    };
  });
}

export function temHorarios(semana: readonly LinhaHorario[]): boolean {
  return semana.some((linha) => linha.intervalo !== null);
}

/** Resumo do dia atual: `"Hoje, 18:00 às 23:00"` ou `"Fechado hoje"`. */
export function resumoDeHoje(semana: readonly LinhaHorario[]): string {
  const intervalo = semana.find((linha) => linha.hoje)?.intervalo;
  return intervalo ? `Hoje, ${intervalo.abertura} às ${intervalo.fechamento}` : 'Fechado hoje';
}

const MINUTOS_POR_HORA = 60;

const formatadorHoraMinuto = new Intl.DateTimeFormat('en-US', {
  timeZone: FUSO_HORARIO,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

/** Dia da semana e minuto do dia (0 a 1439) no fuso dos horários de funcionamento. */
export interface Momento {
  dia: DiaSemana;
  minutos: number;
}

export function momentoEm(instante: Date): Momento {
  const dia = diaDaSemanaEm(instante);
  const partes = formatadorHoraMinuto.formatToParts(instante);
  const valor = (tipo: 'hour' | 'minute') => Number(partes.find((p) => p.type === tipo)?.value);
  return { dia, minutos: valor('hour') * MINUTOS_POR_HORA + valor('minute') };
}

/** `"18:00"` → 1080. */
function minutosDe(horario: string): number {
  const [hora = 0, minuto = 0] = horario.split(':').map(Number);
  return hora * MINUTOS_POR_HORA + minuto;
}

/** Próxima mudança de situação: quando fecha (aberto) ou quando abre de novo (fechado). */
export type Previsao =
  | { tipo: 'fecha'; hora: string }
  | { tipo: 'abre'; quando: 'hoje' | 'amanha' | DiaSemana; hora: string };

/**
 * Previsão como a do protótipo. Quem diz se está aberto é o `abertoAgora` do back; os horários
 * só completam o texto. Aberto: fecha no horário de hoje. Fechado: abre ainda hoje, se o horário
 * de hoje não terminou, ou no próximo dia com horário (até o mesmo dia da semana seguinte).
 * `null` quando os horários não permitem prever (por exemplo, nenhum dia cadastrado).
 *
 * "Não terminou", e não "não começou": o `abertoAgora` usa o relógio do servidor e o `agora`, o do
 * aparelho. Se o aparelho estiver um pouco adiantado na hora de abrir, o back ainda diz fechado e o
 * horário de hoje já parece ter começado; o texto deve ser "abre hoje", não "abre amanhã".
 */
export function previsaoDeFuncionamento(
  horarios: readonly HorarioFuncionamento[],
  abertoAgora: boolean,
  agora: Date,
): Previsao | null {
  const { dia: hoje, minutos } = momentoEm(agora);
  const intervaloDe = (dia: DiaSemana) => {
    const horario = horarios.find((h) => h.diaSemana === dia);
    return horario ? lerIntervalo(horario) : null;
  };
  const deHoje = intervaloDe(hoje);

  if (abertoAgora) {
    return deHoje ? { tipo: 'fecha', hora: deHoje.fechamento } : null;
  }
  if (deHoje && minutos < minutosDe(deHoje.fechamento)) {
    return { tipo: 'abre', quando: 'hoje', hora: deHoje.abertura };
  }
  const indiceDeHoje = DIAS_DA_SEMANA.indexOf(hoje);
  const proximosDias = [
    ...DIAS_DA_SEMANA.slice(indiceDeHoje + 1),
    ...DIAS_DA_SEMANA.slice(0, indiceDeHoje + 1),
  ];
  for (const [distancia, dia] of proximosDias.entries()) {
    const intervalo = intervaloDe(dia);
    if (intervalo) {
      return { tipo: 'abre', quando: distancia === 0 ? 'amanha' : dia, hora: intervalo.abertura };
    }
  }
  return null;
}

/** `"fecha às 23:30"`, `"abre hoje às 18:00"`, `"abre amanhã às 11:00"` ou `"abre sexta às 11:00"`. */
export function textoPrevisao(previsao: Previsao): string {
  if (previsao.tipo === 'fecha') {
    return `fecha às ${previsao.hora}`;
  }
  const quando =
    previsao.quando === 'hoje'
      ? 'hoje'
      : previsao.quando === 'amanha'
        ? 'amanhã'
        : ROTULOS_DIA_SEMANA[previsao.quando].toLowerCase();
  return `abre ${quando} às ${previsao.hora}`;
}
