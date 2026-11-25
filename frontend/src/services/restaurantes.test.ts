import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from './http';
import { buscarRestaurante, chavesRestaurantes, listarHorarios } from './restaurantes';

const ORIGEM = 'http://localhost:8004';

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('serviço de Restaurantes', () => {
  const fetchFalso = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubEnv('VITE_API_RESTAURANTES_URL', ORIGEM);
    vi.stubGlobal('fetch', fetchFalso);
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    fetchFalso.mockReset();
  });

  it('busca o restaurante pelo id', async () => {
    fetchFalso.mockResolvedValue(respostaJson(200, { restauranteId: 7 }));

    await expect(buscarRestaurante(7)).resolves.toEqual({ restauranteId: 7 });
    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/restaurantes/7`);
  });

  it('propaga o 404 do restaurante inexistente como ApiError', async () => {
    fetchFalso.mockResolvedValue(
      respostaJson(404, { erro: true, mensagem: 'Error: Restaurante não encontrado' }),
    );

    const erro = await buscarRestaurante(99).catch((e: unknown) => e);
    expect(erro).toBeInstanceOf(ApiError);
    expect((erro as ApiError).status).toBe(404);
  });

  it('lista os horários do restaurante', async () => {
    const horarios = [
      { diaSemana: 'SEGUNDA', horaAbertura: '18:00:00', horaFechamento: '23:00:00' },
    ];
    fetchFalso.mockResolvedValue(respostaJson(200, horarios));

    await expect(listarHorarios(7)).resolves.toEqual(horarios);
    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/restaurantes/7/horarios`);
  });
});

describe('chavesRestaurantes', () => {
  it('aninha detalhe e horários sob a chave da listagem', () => {
    expect(chavesRestaurantes.detalhe(7)).toEqual(['restaurantes', 7]);
    expect(chavesRestaurantes.horarios(7)).toEqual(['restaurantes', 7, 'horarios']);
  });
});
