/** Fuso dos horários de funcionamento e das datas mostradas ao usuário (igual ao back). */
export const FUSO_HORARIO = 'America/Recife';

/** Instante ISO 8601 em UTC, com `Z` (ex.: `2026-08-24T14:30:00Z`). */
const INSTANTE_UTC = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?Z$/;
/** Horário local `HH:mm:ss` (ou `HH:mm`). */
const HORARIO = /^([01]\d|2[0-3]):([0-5]\d)(:[0-5]\d)?$/;

const formatadorData = new Intl.DateTimeFormat('pt-BR', {
  timeZone: FUSO_HORARIO,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

const formatadorHora = new Intl.DateTimeFormat('pt-BR', {
  timeZone: FUSO_HORARIO,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

function paraData(instante: string): Date {
  const data = new Date(instante);
  if (!INSTANTE_UTC.test(instante) || Number.isNaN(data.getTime())) {
    throw new RangeError(`Instante inválido: "${instante}". Use ISO 8601 em UTC com Z.`);
  }
  return data;
}

/** `2026-08-24T14:30:00Z` → `"24/08/2026"` (data em America/Recife). */
export function formatarData(instante: string): string {
  return formatadorData.format(paraData(instante));
}

/** `2026-08-24T14:30:00Z` → `"24/08/2026 11:30"` (data e hora em America/Recife). */
export function formatarDataHora(instante: string): string {
  const data = paraData(instante);
  return `${formatadorData.format(data)} ${formatadorHora.format(data)}`;
}

/** Horário de funcionamento `18:00:00` → `"18:00"`. */
export function formatarHorario(horario: string): string {
  const partes = HORARIO.exec(horario);
  if (!partes) {
    throw new RangeError(`Horário inválido: "${horario}". Use HH:mm:ss.`);
  }
  return `${partes[1]}:${partes[2]}`;
}
