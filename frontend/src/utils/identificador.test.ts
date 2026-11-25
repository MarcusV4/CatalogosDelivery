import { describe, expect, it } from 'vitest';
import { lerIdDaRota } from './identificador';

describe('lerIdDaRota', () => {
  it('converte um inteiro positivo', () => {
    expect(lerIdDaRota('1')).toBe(1);
    expect(lerIdDaRota('12345')).toBe(12345);
  });

  it.each([undefined, '', '0', '-1', '1.5', '1e3', '01', ' 1', 'abc', '12abc', '9007199254740993'])(
    'recusa %j',
    (valor) => {
      expect(lerIdDaRota(valor)).toBeNull();
    },
  );
});
