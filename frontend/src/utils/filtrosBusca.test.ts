import { describe, expect, it } from 'vitest';
import {
  FILTROS_INICIAIS,
  TAMANHO_PAGINA,
  comFiltro,
  escreverFiltros,
  lerFiltros,
  limparFiltros,
  paraParametrosItens,
  paraParametrosRestaurantes,
  temFiltros,
  trocarAba,
  type FiltrosBusca,
} from './filtrosBusca';

const ler = (query: string) => lerFiltros(new URLSearchParams(query));

const PRATOS_COMPLETO: FiltrosBusca = {
  aba: 'pratos',
  nome: 'pizza',
  categoria: 'PIZZARIA',
  abertoAgora: true,
  precoMin: 2000,
  precoMax: 3000,
  pagina: 2,
};

describe('lerFiltros', () => {
  it('usa o estado inicial para URL vazia', () => {
    expect(ler('')).toEqual(FILTROS_INICIAIS);
  });

  it('lê todos os filtros da aba Pratos', () => {
    expect(
      ler(
        'tipo=pratos&nome=pizza&categoria=PIZZARIA&abertoAgora=true&precoMin=2000&precoMax=3000&pagina=2',
      ),
    ).toEqual(PRATOS_COMPLETO);
  });

  it('remove espaços das pontas do termo', () => {
    expect(ler('nome=%20%20a%C3%A7a%C3%AD%20').nome).toBe('açaí');
  });

  it.each([
    ['tipo=bebidas', { aba: 'restaurantes' }],
    ['categoria=PIZZA', { categoria: null }],
    ['categoria=pizzaria', { categoria: null }],
    ['categoria=toString', { categoria: null }],
    ['abertoAgora=1', { abertoAgora: false }],
    ['abertoAgora=false', { abertoAgora: false }],
    ['pagina=0', { pagina: 1 }],
    ['pagina=-2', { pagina: 1 }],
    ['pagina=abc', { pagina: 1 }],
    ['pagina=1.5', { pagina: 1 }],
  ])('ignora valor inválido em "%s"', (query, esperado) => {
    expect(ler(query)).toMatchObject(esperado);
  });

  it.each([
    ['precoMin=-1', { precoMin: null }],
    ['precoMin=25,90', { precoMin: null }],
    ['precoMax=abc', { precoMax: null }],
    ['precoMin=0', { precoMin: 0 }],
  ])('valida o preço em centavos em "%s"', (query, esperado) => {
    expect(ler(`tipo=pratos&${query}`)).toMatchObject(esperado);
  });

  it('descarta a faixa com mínimo maior que o máximo', () => {
    expect(ler('tipo=pratos&precoMin=3000&precoMax=2000')).toMatchObject({
      precoMin: null,
      precoMax: null,
    });
  });

  it('ignora a faixa de preço na aba Restaurantes', () => {
    expect(ler('precoMin=2000&precoMax=3000')).toMatchObject({ precoMin: null, precoMax: null });
  });
});

describe('escreverFiltros', () => {
  it('omite os valores iniciais', () => {
    expect(escreverFiltros(FILTROS_INICIAIS).toString()).toBe('');
  });

  it('escreve os filtros que leu (ida e volta)', () => {
    const query = escreverFiltros(PRATOS_COMPLETO);
    expect(lerFiltros(query)).toEqual(PRATOS_COMPLETO);
  });

  it('não escreve preço na aba Restaurantes', () => {
    const params = escreverFiltros({ ...FILTROS_INICIAIS, precoMin: 100 });
    expect(params.has('precoMin')).toBe(false);
  });
});

describe('transições', () => {
  it('um filtro novo volta para a primeira página', () => {
    expect(comFiltro(PRATOS_COMPLETO, { abertoAgora: false })).toMatchObject({
      abertoAgora: false,
      pagina: 1,
    });
  });

  it('trocar para Restaurantes mantém termo e categoria e limpa o preço', () => {
    expect(trocarAba(PRATOS_COMPLETO, 'restaurantes')).toEqual({
      aba: 'restaurantes',
      nome: 'pizza',
      categoria: 'PIZZARIA',
      abertoAgora: true,
      precoMin: null,
      precoMax: null,
      pagina: 1,
    });
  });

  it('trocar para Pratos mantém termo e categoria', () => {
    const restaurantes = {
      ...FILTROS_INICIAIS,
      nome: 'açaí',
      categoria: 'ACAI' as const,
      pagina: 3,
    };
    expect(trocarAba(restaurantes, 'pratos')).toMatchObject({
      aba: 'pratos',
      nome: 'açaí',
      categoria: 'ACAI',
      pagina: 1,
    });
  });

  it('limpar mantém só a aba', () => {
    expect(limparFiltros(PRATOS_COMPLETO)).toEqual({ ...FILTROS_INICIAIS, aba: 'pratos' });
  });

  it('temFiltros ignora aba, página e termo em branco', () => {
    expect(temFiltros({ ...FILTROS_INICIAIS, aba: 'pratos', pagina: 4, nome: '  ' })).toBe(false);
    expect(temFiltros({ ...FILTROS_INICIAIS, precoMax: 0 })).toBe(true);
  });
});

describe('parâmetros da API', () => {
  it('restaurantes: categoria em "categoria", página começando em 0', () => {
    expect(paraParametrosRestaurantes(PRATOS_COMPLETO)).toEqual({
      nome: 'pizza',
      abertoAgora: true,
      categoria: 'PIZZARIA',
      page: 1,
      size: TAMANHO_PAGINA,
    });
  });

  it('itens: categoria do restaurante em "categoriaRestaurante" e preços em centavos', () => {
    expect(paraParametrosItens(PRATOS_COMPLETO)).toEqual({
      nome: 'pizza',
      abertoAgora: true,
      categoriaRestaurante: 'PIZZARIA',
      precoMin: 2000,
      precoMax: 3000,
      page: 1,
      size: TAMANHO_PAGINA,
    });
  });

  it('omite filtros vazios e nunca envia abertoAgora=false', () => {
    expect(paraParametrosItens({ ...FILTROS_INICIAIS, aba: 'pratos' })).toEqual({
      nome: undefined,
      abertoAgora: undefined,
      categoriaRestaurante: undefined,
      precoMin: undefined,
      precoMax: undefined,
      page: 0,
      size: TAMANHO_PAGINA,
    });
  });
});
