import { describe, expect, it } from 'vitest';
import type { Previsao } from './horarios';
import { avisoDeFechado, situacaoDoRestaurante } from './situacao';

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

const FECHA_23H30: Previsao = { tipo: 'fecha', hora: '23:30' };
const ABRE_AMANHA: Previsao = { tipo: 'abre', quando: 'amanha', hora: '11:00' };
const ABERTO = { abertoAgora: true, statusRestaurante: 'ABERTO' } as const;
const FORA_DO_HORARIO = { abertoAgora: false, statusRestaurante: 'ABERTO' } as const;
const FECHADO_PELO_DONO = { abertoAgora: false, statusRestaurante: 'FECHADO' } as const;
const INDISPONIVEL = { abertoAgora: false, statusRestaurante: 'INDISPONIVEL' } as const;

describe('situacaoDoRestaurante com a previsão dos horários', () => {
  it('aberto mostra quando fecha', () => {
    expect(situacaoDoRestaurante(ABERTO, FECHA_23H30).rotulo).toBe('Aberto · fecha às 23:30');
  });

  it('fechado pelo horário mostra quando abre', () => {
    expect(situacaoDoRestaurante(FORA_DO_HORARIO, ABRE_AMANHA)).toEqual({
      tipo: 'fechado',
      rotulo: 'Fechado · abre amanhã às 11:00',
    });
  });

  it('fechado pelo dono não tem previsão de abertura', () => {
    expect(situacaoDoRestaurante(FECHADO_PELO_DONO, ABRE_AMANHA).rotulo).toBe('Fechado agora');
  });

  it('indisponível ignora a previsão', () => {
    expect(situacaoDoRestaurante(INDISPONIVEL, ABRE_AMANHA).rotulo).toBe('Indisponível');
  });

  it('sem previsão, ou com uma que não combina com a situação, mantém o texto curto', () => {
    expect(situacaoDoRestaurante(ABERTO, null).rotulo).toBe('Aberto agora');
    expect(situacaoDoRestaurante(ABERTO, ABRE_AMANHA).rotulo).toBe('Aberto agora');
    expect(situacaoDoRestaurante(FORA_DO_HORARIO, null).rotulo).toBe('Fechado agora');
    expect(situacaoDoRestaurante(FORA_DO_HORARIO, FECHA_23H30).rotulo).toBe('Fechado agora');
  });
});

describe('avisoDeFechado', () => {
  it('não mostra aviso com o restaurante aberto', () => {
    expect(avisoDeFechado(ABERTO, FECHA_23H30)).toBeNull();
  });

  it('inclui a previsão de abertura quando fechado pelo horário', () => {
    expect(avisoDeFechado(FORA_DO_HORARIO, ABRE_AMANHA)).toBe(
      'Este restaurante está fechado agora (abre amanhã às 11:00). ' +
        'Você pode ver o cardápio, mas não pedir.',
    );
  });

  it('usa o texto sem previsão quando fechado pelo dono ou sem horários', () => {
    const semPrevisao =
      'Este restaurante está fechado agora. Você pode ver o cardápio, mas não pedir.';
    expect(avisoDeFechado(FECHADO_PELO_DONO, ABRE_AMANHA)).toBe(semPrevisao);
    expect(avisoDeFechado(FORA_DO_HORARIO, null)).toBe(semPrevisao);
  });

  it('avisa quando o restaurante está indisponível', () => {
    expect(avisoDeFechado(INDISPONIVEL, ABRE_AMANHA)).toBe(
      'Este restaurante está temporariamente indisponível. Você pode ver o cardápio, mas não pedir.',
    );
  });
});
