import { describe, expect, it } from 'vitest';
import type { HorarioFuncionamento } from '../services/restaurantes';
import { diaDaSemanaEm, montarSemana, resumoDeHoje, temHorarios, textoIntervalo } from './horarios';

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
