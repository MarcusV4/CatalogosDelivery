import { describe, expect, it } from 'vitest';
import {
  FILTROS_INICIAIS,
  TAMANHO_PAGINA,
  comFiltro,
  escreverFiltros,
  lerFiltros,
  limparFiltros,
  opcoesDeOrdem,
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
  entregaGratis: true,
  precoMin: 2000,
  precoMax: 3000,
  disponivel: true,
  ordem: 'maior-preco',
  pagina: 2,
};

const RESTAURANTES_POR_TAXA: FiltrosBusca = {
  ...FILTROS_INICIAIS,
  entregaGratis: true,
  ordem: 'taxa',
};

describe('lerFiltros', () => {
  it('usa o estado inicial para URL vazia', () => {
    expect(ler('')).toEqual(FILTROS_INICIAIS);
  });

  it('lê todos os filtros da aba Pratos', () => {
    expect(
      ler(
        'tipo=pratos&nome=pizza&categoria=PIZZARIA&abertoAgora=true&entregaGratis=true' +
          '&precoMin=2000&precoMax=3000&disponivel=true&ordem=maior-preco&pagina=2',
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
    ['entregaGratis=1', { entregaGratis: false }],
    ['entregaGratis=false', { entregaGratis: false }],
    ['ordem=menor-preco', { ordem: null }],
    ['ordem=preco,desc', { ordem: null }],
    ['ordem=toString', { ordem: null }],
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

  it('ignora a faixa de preço e "só disponíveis" na aba Restaurantes', () => {
    expect(ler('precoMin=2000&precoMax=3000&disponivel=true')).toMatchObject({
      precoMin: null,
      precoMax: null,
      disponivel: false,
    });
  });

  it.each([
    ['tipo=pratos&disponivel=false', { disponivel: false }],
    ['tipo=pratos&ordem=taxa', { ordem: null }],
    ['tipo=pratos&ordem=menor-preco', { ordem: 'menor-preco' }],
    ['ordem=tempo', { ordem: 'tempo' }],
  ])('só aceita a ordenação e "só disponíveis" válidos para a aba em "%s"', (query, esperado) => {
    expect(ler(query)).toMatchObject(esperado);
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

  it('ida e volta na aba Restaurantes, com ordenação', () => {
    expect(escreverFiltros(RESTAURANTES_POR_TAXA).toString()).toBe('entregaGratis=true&ordem=taxa');
    expect(lerFiltros(escreverFiltros(RESTAURANTES_POR_TAXA))).toEqual(RESTAURANTES_POR_TAXA);
  });

  it('não escreve o que só vale em Pratos, nem ordenação de outra aba, na aba Restaurantes', () => {
    const params = escreverFiltros({
      ...FILTROS_INICIAIS,
      precoMin: 100,
      disponivel: true,
      ordem: 'menor-preco',
    });
    expect(params.toString()).toBe('');
  });
});

describe('transições', () => {
  it('um filtro novo volta para a primeira página', () => {
    expect(comFiltro(PRATOS_COMPLETO, { abertoAgora: false })).toMatchObject({
      abertoAgora: false,
      pagina: 1,
    });
  });

  it('trocar para Restaurantes mantém termo, categoria e filtros do restaurante e limpa o resto', () => {
    expect(trocarAba(PRATOS_COMPLETO, 'restaurantes')).toEqual({
      aba: 'restaurantes',
      nome: 'pizza',
      categoria: 'PIZZARIA',
      abertoAgora: true,
      entregaGratis: true,
      precoMin: null,
      precoMax: null,
      disponivel: false,
      ordem: null,
      pagina: 1,
    });
  });

  it('trocar para Pratos mantém "entrega grátis" e limpa a ordenação de Restaurantes', () => {
    expect(trocarAba(RESTAURANTES_POR_TAXA, 'pratos')).toMatchObject({
      aba: 'pratos',
      entregaGratis: true,
      ordem: null,
    });
  });

  it('mudar a ordenação volta para a primeira página', () => {
    expect(comFiltro({ ...PRATOS_COMPLETO, pagina: 3 }, { ordem: 'menor-preco' })).toMatchObject({
      ordem: 'menor-preco',
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

  it('temFiltros conta "entrega grátis" e "só disponíveis", mas não a ordenação', () => {
    expect(temFiltros({ ...FILTROS_INICIAIS, entregaGratis: true })).toBe(true);
    expect(temFiltros({ ...FILTROS_INICIAIS, aba: 'pratos', disponivel: true })).toBe(true);
    expect(temFiltros({ ...FILTROS_INICIAIS, ordem: 'tempo' })).toBe(false);
  });

  it('limpar também zera a ordenação', () => {
    expect(limparFiltros(RESTAURANTES_POR_TAXA)).toEqual(FILTROS_INICIAIS);
  });
});

describe('parâmetros da API', () => {
  it('restaurantes: categoria em "categoria", página começando em 0', () => {
    expect(paraParametrosRestaurantes(PRATOS_COMPLETO)).toEqual({
      nome: 'pizza',
      abertoAgora: true,
      entregaGratis: true,
      categoria: 'PIZZARIA',
      sort: undefined,
      page: 1,
      size: TAMANHO_PAGINA,
    });
  });

  it.each([
    ['taxa', 'taxaEntrega,asc'],
    ['tempo', 'tempoEstimado,asc'],
  ] as const)('restaurantes: ordenação "%s" vira sort=%s', (ordem, sort) => {
    expect(paraParametrosRestaurantes({ ...FILTROS_INICIAIS, ordem }).sort).toBe(sort);
  });

  it.each([
    ['menor-preco', 'preco,asc'],
    ['maior-preco', 'preco,desc'],
  ] as const)('itens: ordenação "%s" vira sort=%s', (ordem, sort) => {
    expect(paraParametrosItens({ ...FILTROS_INICIAIS, aba: 'pratos', ordem }).sort).toBe(sort);
  });

  it('itens: categoria do restaurante em "categoriaRestaurante" e preços em centavos', () => {
    expect(paraParametrosItens(PRATOS_COMPLETO)).toEqual({
      nome: 'pizza',
      abertoAgora: true,
      entregaGratis: true,
      categoriaRestaurante: 'PIZZARIA',
      precoMin: 2000,
      precoMax: 3000,
      disponivel: true,
      sort: 'preco,desc',
      page: 1,
      size: TAMANHO_PAGINA,
    });
  });

  it('omite filtros vazios e nunca envia false nem a ordenação padrão', () => {
    expect(paraParametrosItens({ ...FILTROS_INICIAIS, aba: 'pratos' })).toEqual({
      nome: undefined,
      abertoAgora: undefined,
      entregaGratis: undefined,
      categoriaRestaurante: undefined,
      precoMin: undefined,
      precoMax: undefined,
      disponivel: undefined,
      sort: undefined,
      page: 0,
      size: TAMANHO_PAGINA,
    });
  });
});

describe('opcoesDeOrdem', () => {
  it('começa pela ordenação padrão e traz só as opções da aba', () => {
    expect(opcoesDeOrdem('restaurantes')).toEqual([
      { valor: '', rotulo: 'Nome (A–Z)' },
      { valor: 'taxa', rotulo: 'Menor taxa de entrega' },
      { valor: 'tempo', rotulo: 'Menor tempo de entrega' },
    ]);
    expect(opcoesDeOrdem('pratos').map((opcao) => opcao.valor)).toEqual([
      '',
      'menor-preco',
      'maior-preco',
    ]);
  });
});
