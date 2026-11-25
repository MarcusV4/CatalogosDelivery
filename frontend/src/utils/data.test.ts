import { describe, expect, it } from 'vitest';
import { formatarData, formatarDataHora, formatarHorario } from './data';

describe('formatarDataHora', () => {
  it('converte o instante UTC para a hora de Recife (UTC−3)', () => {
    expect(formatarDataHora('2026-08-24T14:30:00Z')).toBe('24/08/2026 11:30');
  });

  it('volta um dia quando o instante UTC é de madrugada', () => {
    expect(formatarDataHora('2026-08-25T02:30:00Z')).toBe('24/08/2026 23:30');
    expect(formatarDataHora('2027-01-01T00:00:00Z')).toBe('31/12/2026 21:00');
  });

  it('aceita frações de segundo e instantes sem segundos', () => {
    expect(formatarDataHora('2026-08-24T14:30:59.123456Z')).toBe('24/08/2026 11:30');
    expect(formatarDataHora('2026-08-24T14:30Z')).toBe('24/08/2026 11:30');
  });

  it.each([
    '',
    'ontem',
    '24/08/2026',
    '2026-08-24T14:30:00',
    '2026-08-24T14:30:00-03:00',
    '2026-13-40T99:99:00Z',
  ])('recusa "%s"', (entrada) => {
    expect(() => formatarDataHora(entrada)).toThrow(RangeError);
  });
});

describe('formatarData', () => {
  it('usa a data local de Recife', () => {
    expect(formatarData('2026-08-24T14:30:00Z')).toBe('24/08/2026');
    expect(formatarData('2026-08-25T02:59:59Z')).toBe('24/08/2026');
    expect(formatarData('2026-08-25T03:00:00Z')).toBe('25/08/2026');
  });
});

describe('formatarHorario', () => {
  it.each([
    ['18:00:00', '18:00'],
    ['00:00:00', '00:00'],
    ['23:59:59', '23:59'],
    ['09:05', '09:05'],
  ])('%s → %s', (entrada, esperado) => {
    expect(formatarHorario(entrada)).toBe(esperado);
  });

  it.each(['24:00:00', '9:00', '18h', '18:60:00', ''])('recusa "%s"', (entrada) => {
    expect(() => formatarHorario(entrada)).toThrow(RangeError);
  });
});
