import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { chavesCardapio, listarCardapio } from './cardapio';

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
});
