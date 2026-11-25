import { describe, expect, it } from 'vitest';
import type { CategoriaProduto, Produto } from '../services/cardapio';
import {
  ORDEM_CATEGORIAS_PRODUTO,
  agruparPorCategoria,
  idDaSecaoCategoria,
  rotuloCategoriaProduto,
} from './cardapio';
import { ROTULOS_CATEGORIA_PRODUTO } from './rotulos';

function produto(id: number, categoria: string, extras: Partial<Produto> = {}): Produto {
  return {
    id,
    nome: `Produto ${id}`,
    descricao: null,
    preco: 1000,
    disponivel: true,
    // Simula o back enviando um valor que o front pode ainda não conhecer.
    categoria: categoria as CategoriaProduto,
    restauranteId: 1,
    urlImagem: null,
    adicionais: [],
    ...extras,
  };
}

const categorias = (grupos: ReturnType<typeof agruparPorCategoria>) =>
  grupos.map((grupo) => grupo.categoria);

describe('agruparPorCategoria', () => {
  it('devolve lista vazia para cardápio vazio', () => {
    expect(agruparPorCategoria([])).toEqual([]);
  });

  it('agrupa uma categoria única com o rótulo acentuado', () => {
    const grupos = agruparPorCategoria([produto(1, 'PIZZA'), produto(2, 'PIZZA')]);

    expect(grupos).toHaveLength(1);
    expect(grupos[0]?.rotulo).toBe('Pizzas');
    expect(grupos[0]?.produtos.map((p) => p.id)).toEqual([1, 2]);
  });

  it('ordena várias categorias pela ordem de exibição, não pela ordem do enum', () => {
    const grupos = agruparPorCategoria([
      produto(1, 'BEBIDA'),
      produto(2, 'SOBREMESA'),
      produto(3, 'PIZZA'),
      produto(4, 'ENTRADA'),
      produto(5, 'PRATO_PRINCIPAL'),
    ]);

    expect(categorias(grupos)).toEqual([
      'ENTRADA',
      'PRATO_PRINCIPAL',
      'PIZZA',
      'SOBREMESA',
      'BEBIDA',
    ]);
  });

  it('põe categorias desconhecidas no fim, em ordem alfabética, sem erro', () => {
    const grupos = agruparPorCategoria([
      produto(1, 'ZETA_NOVA'),
      produto(2, 'BEBIDA'),
      produto(3, 'ALFA_NOVA'),
      produto(4, 'ENTRADA'),
    ]);

    expect(categorias(grupos)).toEqual(['ENTRADA', 'BEBIDA', 'ALFA_NOVA', 'ZETA_NOVA']);
    expect(grupos[2]?.rotulo).toBe('Alfa nova');
  });

  it('ordena os itens de cada categoria pelo id, qualquer que seja a ordem da resposta', () => {
    const grupos = agruparPorCategoria([
      produto(9, 'PIZZA'),
      produto(3, 'PIZZA'),
      produto(5, 'PIZZA'),
    ]);

    expect(grupos[0]?.produtos.map((p) => p.id)).toEqual([3, 5, 9]);
  });

  it('mantém itens indisponíveis e sem urlImagem', () => {
    const indisponivel = produto(1, 'PIZZA', { disponivel: false });
    const semImagem = produto(2, 'PIZZA', { urlImagem: null });

    const [grupo] = agruparPorCategoria([indisponivel, semImagem]);

    expect(grupo?.produtos).toEqual([indisponivel, semImagem]);
    expect(grupo?.produtos[0]?.disponivel).toBe(false);
  });

  it('não altera a lista recebida', () => {
    const lista = [produto(2, 'PIZZA'), produto(1, 'PIZZA')];

    agruparPorCategoria(lista);

    expect(lista.map((p) => p.id)).toEqual([2, 1]);
  });
});

describe('ORDEM_CATEGORIAS_PRODUTO', () => {
  it('cobre todas as categorias do back, sem repetir', () => {
    expect([...ORDEM_CATEGORIAS_PRODUTO].sort()).toEqual(
      Object.keys(ROTULOS_CATEGORIA_PRODUTO).sort(),
    );
  });
});

describe('rotuloCategoriaProduto', () => {
  it('usa o rótulo conhecido ou deixa o valor legível', () => {
    expect(rotuloCategoriaProduto('PRATO_PRINCIPAL')).toBe('Pratos principais');
    expect(rotuloCategoriaProduto('COMIDA_DE_RUA')).toBe('Comida de rua');
  });

  it('não confunde propriedades herdadas com categorias', () => {
    expect(rotuloCategoriaProduto('toString')).toBe('Tostring');
  });
});

describe('idDaSecaoCategoria', () => {
  it('gera um id de âncora estável e em minúsculas', () => {
    expect(idDaSecaoCategoria('PRATO_PRINCIPAL')).toBe('categoria-prato_principal');
  });
});
