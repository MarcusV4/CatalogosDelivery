import { describe, expect, it } from 'vitest';
import { formatarPreco } from './preco';

/** O Intl usa espaço não separável entre "R$" e o número. */
const reais = (texto: string) => texto.replace(' ', ' ');

describe('formatarPreco', () => {
  it.each([
    [0, 'R$ 0,00'],
    [5, 'R$ 0,05'],
    [99, 'R$ 0,99'],
    [100, 'R$ 1,00'],
    [2590, 'R$ 25,90'],
    [123456789, 'R$ 1.234.567,89'],
    [Number.MAX_SAFE_INTEGER, 'R$ 90.071.992.547.409,91'],
  ])('%i centavos → %s', (centavos, esperado) => {
    expect(formatarPreco(centavos)).toBe(reais(esperado));
  });

  it.each([25.9, 0.5, -1, Number.NaN, Number.POSITIVE_INFINITY, Number.MAX_SAFE_INTEGER + 1])(
    'recusa %s',
    (valor) => {
      expect(() => formatarPreco(valor)).toThrow(RangeError);
    },
  );

  it('recusa valores que não são número', () => {
    expect(() => formatarPreco('2590' as unknown as number)).toThrow(RangeError);
  });
});
