import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { buscarItens, buscarRestaurantes, chavesBusca, montarQuery } from './busca';

const ORIGEM = 'http://localhost:8006';
const PAGINA_VAZIA = { conteudo: [], pagina: 0, tamanho: 12, totalElementos: 0, totalPaginas: 0 };

describe('montarQuery', () => {
  it('omite parâmetros indefinidos e codifica os valores', () => {
    expect(
      montarQuery({ nome: 'açaí e pão', categoria: undefined, abertoAgora: true, page: 0 }),
    ).toBe('nome=a%C3%A7a%C3%AD+e+p%C3%A3o&abertoAgora=true&page=0');
  });

  it('devolve texto vazio sem parâmetros', () => {
    expect(montarQuery({})).toBe('');
  });
});

describe('serviço de Busca', () => {
  const fetchFalso = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubEnv('VITE_API_BUSCA_URL', ORIGEM);
    vi.stubGlobal('fetch', fetchFalso);
    fetchFalso.mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify(PAGINA_VAZIA), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    fetchFalso.mockReset();
  });

  it('busca restaurantes com os parâmetros da API', async () => {
    await expect(
      buscarRestaurantes({
        nome: 'pizza',
        categoria: 'PIZZARIA',
        abertoAgora: true,
        page: 1,
        size: 12,
      }),
    ).resolves.toEqual(PAGINA_VAZIA);
    expect(fetchFalso.mock.calls[0]?.[0]).toBe(
      `${ORIGEM}/api/busca/restaurantes?nome=pizza&categoria=PIZZARIA&abertoAgora=true&page=1&size=12`,
    );
  });

  it('busca itens com faixa de preço em centavos', async () => {
    await buscarItens({
      categoriaRestaurante: 'ACAI',
      precoMin: 1000,
      precoMax: 2590,
      page: 0,
      size: 12,
    });
    expect(fetchFalso.mock.calls[0]?.[0]).toBe(
      `${ORIGEM}/api/busca/itens?categoriaRestaurante=ACAI&precoMin=1000&precoMax=2590&page=0&size=12`,
    );
  });
});

describe('chavesBusca', () => {
  it('separa restaurantes e itens sob a mesma raiz', () => {
    const parametros = { page: 0, size: 12 };
    expect(chavesBusca.restaurantes(parametros)).toEqual(['busca', 'restaurantes', parametros]);
    expect(chavesBusca.itens(parametros)).toEqual(['busca', 'itens', parametros]);
  });
});
