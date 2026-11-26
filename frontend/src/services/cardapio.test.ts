import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  alterarDisponibilidadeProduto,
  atualizarProduto,
  chavesCardapio,
  criarProduto,
  excluirProduto,
  listarCardapio,
  listarCategorias,
} from './cardapio';

const ORIGEM = 'http://localhost:8005';

describe('serviço de Cardápio', () => {
  const fetchFalso = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubEnv('VITE_API_CARDAPIO_URL', ORIGEM);
    vi.stubGlobal('fetch', fetchFalso);
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    fetchFalso.mockReset();
  });

  it('lista o cardápio do restaurante', async () => {
    fetchFalso.mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(listarCardapio(3)).resolves.toEqual([]);
    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/restaurantes/3/cardapio`);
  });

  it('usa uma chave de cache por restaurante', () => {
    expect(chavesCardapio.doRestaurante(3)).toEqual(['cardapio', 'restaurante', 3]);
  });

  it('lista as categorias disponíveis para produtos', async () => {
    fetchFalso.mockResolvedValue(
      new Response(JSON.stringify(['PIZZA', 'BEBIDA']), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(listarCategorias()).resolves.toEqual(['PIZZA', 'BEBIDA']);
    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/categorias`);
  });

  it('cria produto no cardápio selecionado e envia preço em centavos', async () => {
    fetchFalso.mockResolvedValue(
      new Response(JSON.stringify({ id: 7 }), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await criarProduto(3, {
      nome: 'Pizza Recife',
      descricao: 'Mussarela',
      preco: 2590,
      disponivel: true,
      urlImagem: null,
      categoria: 'PIZZA',
    });

    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/restaurantes/3/cardapio`);
    expect(fetchFalso.mock.calls[0]?.[1]).toMatchObject({
      method: 'POST',
      body: JSON.stringify({
        nome: 'Pizza Recife',
        descricao: 'Mussarela',
        preco: 2590,
        disponivel: true,
        urlImagem: null,
        categoria: 'PIZZA',
      }),
    });
  });

  it('atualiza produto com todos os campos pelo endpoint de edição', async () => {
    fetchFalso.mockResolvedValue(
      new Response(JSON.stringify({ id: 7 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await atualizarProduto(7, {
      nome: 'Pizza nova',
      descricao: null,
      preco: 3000,
      disponivel: true,
      urlImagem: null,
      categoria: 'PIZZA',
    });

    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/produtos/7`);
    expect(fetchFalso.mock.calls[0]?.[1]).toMatchObject({ method: 'PUT' });
  });

  it('altera disponibilidade pelo endpoint dedicado', async () => {
    fetchFalso.mockResolvedValue(
      new Response(JSON.stringify({ id: 7, disponivel: false }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await alterarDisponibilidadeProduto(7, false);

    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/produtos/7/disponibilidade`);
    expect(fetchFalso.mock.calls[0]?.[1]).toMatchObject({
      method: 'PATCH',
      body: JSON.stringify({ disponivel: false }),
    });
  });

  it('exclui produto e aceita resposta sem corpo', async () => {
    fetchFalso.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(excluirProduto(7)).resolves.toBeUndefined();
    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/produtos/7`);
    expect(fetchFalso.mock.calls[0]?.[1]).toMatchObject({ method: 'DELETE' });
  });
});
