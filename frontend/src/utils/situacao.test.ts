import { describe, expect, it } from 'vitest';
import { situacaoDoRestaurante } from './situacao';

describe('situacaoDoRestaurante', () => {
  it('mostra "Aberto agora" quando o back calcula abertoAgora', () => {
    expect(situacaoDoRestaurante({ abertoAgora: true, statusRestaurante: 'ABERTO' })).toEqual({
      tipo: 'aberto',
      rotulo: 'Aberto agora',
    });
  });

  it('mostra "Fechado agora" para status ABERTO fora do horário e para FECHADO', () => {
    for (const statusRestaurante of ['ABERTO', 'FECHADO'] as const) {
      expect(situacaoDoRestaurante({ abertoAgora: false, statusRestaurante })).toEqual({
        tipo: 'fechado',
        rotulo: 'Fechado agora',
      });
    }
  });

  it('mostra "Indisponível" para restaurante desativado', () => {
    expect(
      situacaoDoRestaurante({ abertoAgora: false, statusRestaurante: 'INDISPONIVEL' }),
    ).toEqual({ tipo: 'indisponivel', rotulo: 'Indisponível' });
  });
});
