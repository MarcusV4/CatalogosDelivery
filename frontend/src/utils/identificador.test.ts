import { describe, expect, it } from 'vitest';
import { lerIdDaRota, lerInteiroNaoNegativo } from './identificador';

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

describe('lerInteiroNaoNegativo', () => {
  it('aceita zero e inteiros positivos', () => {
    expect(lerInteiroNaoNegativo('0')).toBe(0);
    expect(lerInteiroNaoNegativo('2590')).toBe(2590);
  });

  it.each([null, undefined, '', '-1', '00', '1.5', 'abc', '9007199254740993'])(
    'recusa %j',
    (valor) => {
      expect(lerInteiroNaoNegativo(valor)).toBeNull();
    },
  );
});
