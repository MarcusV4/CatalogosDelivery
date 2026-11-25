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
