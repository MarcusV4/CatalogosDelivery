import { describe, expect, it } from 'vitest';
import { centavosParaReais, reaisParaCentavos } from './precoEntrada';

describe('reaisParaCentavos', () => {
  it.each([
    ['25,90', 2590],
    ['25.90', 2590],
    ['25,9', 2590],
    ['25', 2500],
    ['0', 0],
    ['0,05', 5],
    [' 18,00 ', 1800],
    ['007', 700],
    ['90071992547409,91', Number.MAX_SAFE_INTEGER],
  ])('"%s" → %i centavos', (texto, esperado) => {
    expect(reaisParaCentavos(texto)).toBe(esperado);
  });

  it.each([
    '',
    '   ',
    '-5',
    '-0,50',
    'abc',
    '25,',
    ',50',
    '25,999',
    '1.234,56',
    '1,234.56',
    '25 reais',
    'R$ 25',
    '1e3',
    '90071992547409,92',
  ])('recusa "%s"', (texto) => {
    expect(reaisParaCentavos(texto)).toBeNull();
  });
});

describe('centavosParaReais', () => {
  it.each([
    [2590, '25,90'],
    [2500, '25'],
    [5, '0,05'],
    [0, '0'],
    [Number.MAX_SAFE_INTEGER, '90071992547409,91'],
  ])('%i → "%s"', (centavos, esperado) => {
    expect(centavosParaReais(centavos)).toBe(esperado);
  });

  it('volta ao mesmo valor ao converter de novo', () => {
    for (const centavos of [0, 1, 99, 100, 2590, 123456]) {
      expect(reaisParaCentavos(centavosParaReais(centavos))).toBe(centavos);
    }
  });
});
