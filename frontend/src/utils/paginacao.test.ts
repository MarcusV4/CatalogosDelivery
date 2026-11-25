import { describe, expect, it } from 'vitest';
import { corrigirPagina, estadoPaginacao, textoTotal } from './paginacao';

describe('corrigirPagina', () => {
  it('mantém uma página que existe', () => {
    expect(corrigirPagina(1, 3)).toBeNull();
    expect(corrigirPagina(3, 3)).toBeNull();
  });

  it('leva uma página além da última para a última', () => {
    expect(corrigirPagina(9, 3)).toBe(3);
  });

  it('sem resultados, leva para a página 1 e não redireciona a página 1 (sem laço)', () => {
    expect(corrigirPagina(5, 0)).toBe(1);
    expect(corrigirPagina(1, 0)).toBeNull();
    expect(corrigirPagina(corrigirPagina(5, 0) ?? 5, 0)).toBeNull();
  });
});

describe('estadoPaginacao', () => {
  it('habilita os botões conforme a posição', () => {
    expect(estadoPaginacao(1, 3)).toEqual({
      temAnterior: false,
      temProxima: true,
      texto: 'Página 1 de 3',
    });
    expect(estadoPaginacao(2, 3)).toMatchObject({ temAnterior: true, temProxima: true });
    expect(estadoPaginacao(3, 3)).toMatchObject({ temAnterior: true, temProxima: false });
  });

  it('trata zero páginas como uma só', () => {
    expect(estadoPaginacao(1, 0)).toEqual({
      temAnterior: false,
      temProxima: false,
      texto: 'Página 1 de 1',
    });
  });
});

describe('textoTotal', () => {
  it('concorda em número', () => {
    expect(textoTotal(0, 'prato', 'pratos')).toBe('Nenhum prato encontrado');
    expect(textoTotal(1, 'prato', 'pratos')).toBe('1 prato encontrado');
    expect(textoTotal(42, 'restaurante', 'restaurantes')).toBe('42 restaurantes encontrados');
  });
});
