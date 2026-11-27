import { describe, expect, it } from 'vitest';
import type { DiaSemana, HorarioFuncionamento } from '../services/restaurantes';
import {
  diaDaSemanaEm,
  momentoEm,
  montarSemana,
  previsaoDeFuncionamento,
  resumoDeHoje,
  temHorarios,
  textoIntervalo,
  textoPrevisao,
} from './horarios';

const SEGUNDA_A_NOITE: HorarioFuncionamento = {
  diaSemana: 'SEGUNDA',
  horaAbertura: '18:00:00',
  horaFechamento: '23:00:00',
};

describe('montarSemana', () => {
  it('devolve os 7 dias de domingo a sábado, mesmo com a resposta fora de ordem', () => {
    const semana = montarSemana(
      [
        { diaSemana: 'SABADO', horaAbertura: '10:00:00', horaFechamento: '14:00:00' },
        SEGUNDA_A_NOITE,
      ],
      'SEGUNDA',
    );

    expect(semana.map((linha) => linha.rotulo)).toEqual([
      'Domingo',
      'Segunda',
      'Terça',
      'Quarta',
      'Quinta',
      'Sexta',
      'Sábado',
    ]);
    expect(semana[1]?.intervalo).toEqual({ abertura: '18:00', fechamento: '23:00' });
    expect(semana[6]?.intervalo).toEqual({ abertura: '10:00', fechamento: '14:00' });
  });

  it('marca o dia sem horário como fechado', () => {
    const semana = montarSemana([SEGUNDA_A_NOITE], 'SEGUNDA');

    expect(semana[0]?.intervalo).toBeNull();
    expect(textoIntervalo(semana[0]?.intervalo ?? null)).toBe('Fechado');
    expect(textoIntervalo(semana[1]?.intervalo ?? null)).toBe('18:00 – 23:00');
  });

  it('marca só o dia de hoje', () => {
    const semana = montarSemana([], 'QUARTA');

    expect(semana.filter((linha) => linha.hoje).map((linha) => linha.dia)).toEqual(['QUARTA']);
  });

  it('trata restaurante sem horários', () => {
    const semana = montarSemana([], 'SEGUNDA');

    expect(semana.every((linha) => linha.intervalo === null)).toBe(true);
    expect(temHorarios(semana)).toBe(false);
    expect(temHorarios(montarSemana([SEGUNDA_A_NOITE], 'SEGUNDA'))).toBe(true);
  });

  it('trata horário em formato inválido como dia sem horário, sem quebrar a tela', () => {
    const semana = montarSemana(
      [{ diaSemana: 'SEGUNDA', horaAbertura: '6 da tarde', horaFechamento: '23:00:00' }],
      'SEGUNDA',
    );

    expect(semana[1]?.intervalo).toBeNull();
  });
});

describe('resumoDeHoje', () => {
  it('mostra o horário de hoje ou "Fechado hoje"', () => {
    expect(resumoDeHoje(montarSemana([SEGUNDA_A_NOITE], 'SEGUNDA'))).toBe('Hoje, 18:00 às 23:00');
    expect(resumoDeHoje(montarSemana([SEGUNDA_A_NOITE], 'TERCA'))).toBe('Fechado hoje');
  });
});

describe('diaDaSemanaEm', () => {
  it('usa o dia de Recife (UTC−3), não o de UTC', () => {
    // 2026-11-24 é uma terça; 01:00 UTC de quarta ainda é 22:00 de terça em Recife.
    expect(diaDaSemanaEm(new Date('2026-11-24T15:00:00Z'))).toBe('TERCA');
    expect(diaDaSemanaEm(new Date('2026-11-25T01:00:00Z'))).toBe('TERCA');
    expect(diaDaSemanaEm(new Date('2026-11-25T03:00:00Z'))).toBe('QUARTA');
    expect(diaDaSemanaEm(new Date('2026-11-29T12:00:00Z'))).toBe('DOMINGO');
  });

  it('recusa data inválida', () => {
    expect(() => diaDaSemanaEm(new Date('ontem'))).toThrow(RangeError);
  });
});

describe('momentoEm', () => {
  it('devolve o dia e o minuto do dia em Recife', () => {
    // 01:30 UTC de quarta é 22:30 de terça em Recife.
    expect(momentoEm(new Date('2026-11-25T01:30:00Z'))).toEqual({ dia: 'TERCA', minutos: 1350 });
    expect(momentoEm(new Date('2026-11-25T03:00:00Z'))).toEqual({ dia: 'QUARTA', minutos: 0 });
  });
});

/** 2026-11-24 é uma terça; Recife é UTC−3. */
const TERCA_12H = new Date('2026-11-24T15:00:00Z');
const TERCA_16H = new Date('2026-11-24T19:00:00Z');
const SABADO_15H = new Date('2026-11-28T18:00:00Z');

function horario(diaSemana: DiaSemana, abertura: string, fechamento: string): HorarioFuncionamento {
  return { diaSemana, horaAbertura: `${abertura}:00`, horaFechamento: `${fechamento}:00` };
}

/** Previsão já em texto, como aparece no selo. */
function previsao(
  horarios: HorarioFuncionamento[],
  abertoAgora: boolean,
  agora: Date,
): string | null {
  const resultado = previsaoDeFuncionamento(horarios, abertoAgora, agora);
  return resultado ? textoPrevisao(resultado) : null;
}

describe('previsaoDeFuncionamento', () => {
  it('aberto: fecha no horário de hoje', () => {
    expect(previsao([horario('TERCA', '11:00', '23:30')], true, TERCA_12H)).toBe('fecha às 23:30');
  });

  it('aberto: fechamento às 23:59:59 aparece como 23:59', () => {
    const horarios: HorarioFuncionamento[] = [
      { diaSemana: 'TERCA', horaAbertura: '00:00:00', horaFechamento: '23:59:59' },
    ];
    expect(previsao(horarios, true, TERCA_12H)).toBe('fecha às 23:59');
  });

  it('aberto sem horário para hoje não prevê nada', () => {
    expect(previsao([horario('QUARTA', '11:00', '15:00')], true, TERCA_12H)).toBeNull();
  });

  it('fechado antes do horário de hoje: abre hoje', () => {
    expect(previsao([horario('TERCA', '18:00', '23:00')], false, TERCA_12H)).toBe(
      'abre hoje às 18:00',
    );
  });

  it('fechado com o relógio do aparelho já dentro do horário de hoje: abre hoje, não amanhã', () => {
    // O back calculou às 11:59 (fechado); o aparelho, adiantado, já marca 12:00.
    const horarios = [horario('TERCA', '12:00', '15:00'), horario('QUARTA', '12:00', '15:00')];
    expect(previsao(horarios, false, TERCA_12H)).toBe('abre hoje às 12:00');
  });

  it('fechado depois do horário de hoje: abre amanhã', () => {
    const horarios = [horario('TERCA', '11:00', '15:00'), horario('QUARTA', '10:00', '15:00')];
    expect(previsao(horarios, false, TERCA_16H)).toBe('abre amanhã às 10:00');
  });

  it('pula os dias sem horário e diz o nome do dia, em minúsculas', () => {
    expect(previsao([horario('SEXTA', '11:00', '22:00')], false, TERCA_12H)).toBe(
      'abre sexta às 11:00',
    );
  });

  it('dá a volta na semana, de sábado para domingo e segunda', () => {
    expect(previsao([horario('DOMINGO', '10:00', '14:00')], false, SABADO_15H)).toBe(
      'abre amanhã às 10:00',
    );
    expect(previsao([horario('SEGUNDA', '10:00', '14:00')], false, SABADO_15H)).toBe(
      'abre segunda às 10:00',
    );
  });

  it('só com o horário de hoje, já encerrado: abre no mesmo dia da próxima semana', () => {
    expect(previsao([horario('TERCA', '11:00', '15:00')], false, TERCA_16H)).toBe(
      'abre terça às 11:00',
    );
  });

  it('sem horários não prevê nada', () => {
    expect(previsao([], false, TERCA_12H)).toBeNull();
    expect(previsao([], true, TERCA_12H)).toBeNull();
  });

  it('ignora horário em formato inválido', () => {
    const horarios: HorarioFuncionamento[] = [
      { diaSemana: 'TERCA', horaAbertura: '6 da tarde', horaFechamento: '23:00:00' },
    ];
    expect(previsao(horarios, false, TERCA_12H)).toBeNull();
  });
});
