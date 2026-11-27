import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from './http';
import { definirSessaoAtual, limparSessaoAtual } from './oauth';
import {
  atualizarRestaurante,
  atualizarStatusRestaurante,
  buscarRestaurante,
  chavesRestaurantes,
  criarRestaurante,
  listarHorarios,
  listarRestaurantesDoUsuario,
  substituirHorarios,
} from './restaurantes';

const DONO = '6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01';
const OUTRA_CONTA = '0a9b8c7d-6e5f-4a3b-9c2d-1e0f9a8b7c6d';

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
    limparSessaoAtual();
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

  it('lista somente os restaurantes ativos do usuário com consulta autenticada', async () => {
    definirSessaoAtual({
      accessToken: 'token-loja',
      refreshToken: 'refresh',
      tipo: 'LOJA',
      usuarioId: DONO,
      expiraEm: Date.now() + 60_000,
    });
    fetchFalso.mockResolvedValue(
      respostaJson(200, [
        { restauranteId: 1, ownerUserId: DONO },
        { restauranteId: 2, ownerUserId: OUTRA_CONTA },
      ]),
    );

    await expect(listarRestaurantesDoUsuario(DONO)).resolves.toEqual([
      { restauranteId: 1, ownerUserId: DONO },
    ]);
    expect(fetchFalso.mock.calls[0]?.[1]?.headers).toMatchObject({
      Authorization: 'Bearer token-loja',
    });
  });

  it('cadastra restaurante sem aceitar ownerUserId vindo do front', async () => {
    definirSessaoAtual({
      accessToken: 'token-loja',
      refreshToken: 'refresh',
      tipo: 'LOJA',
      usuarioId: DONO,
      expiraEm: Date.now() + 60_000,
    });
    fetchFalso.mockResolvedValue(respostaJson(201, { restauranteId: 3 }));
    const cadastro = {
      restauranteNome: 'Casa do Acarajé',
      restauranteDesc: 'Comida baiana',
      logradouro: 'Rua A',
      numero: '10',
      complemento: '',
      bairro: 'Centro',
      cidade: 'Recife',
      estado: 'PE',
      cep: '50000-000',
      restauranteTelefone: '81999990000',
      taxaEntrega: 500,
      tempoEstimadoMin: 30,
      tempoEstimadoMax: 50,
      urlImagem: '',
      categoriaRestaurante: 'COMIDA_REGIONAL',
    } as const;

    await criarRestaurante(cadastro);

    expect(fetchFalso.mock.calls[0]?.[0]).toBe(`${ORIGEM}/api/restaurantes`);
    expect(JSON.parse(String(fetchFalso.mock.calls[0]?.[1]?.body))).not.toHaveProperty('ownerUserId');
  });

  it('atualiza dados, status e horários em rotas protegidas sem ownerUserId no corpo', async () => {
    definirSessaoAtual({
      accessToken: 'token-loja',
      refreshToken: 'refresh',
      tipo: 'LOJA',
      usuarioId: DONO,
      expiraEm: Date.now() + 60_000,
    });
    fetchFalso
      .mockResolvedValueOnce(respostaJson(200, { restauranteId: 7 }))
      .mockResolvedValueOnce(respostaJson(200, { restauranteId: 7 }))
      .mockResolvedValueOnce(respostaJson(200, []));
    const dados = {
      restauranteNome: 'Casa do Acarajé',
      restauranteDesc: 'Comida baiana',
      logradouro: 'Rua A',
      numero: '10',
      complemento: '',
      bairro: 'Centro',
      cidade: 'Recife',
      estado: 'PE',
      cep: '50000-000',
      restauranteTelefone: '81999990000',
      taxaEntrega: 500,
      tempoEstimadoMin: 30,
      tempoEstimadoMax: 50,
      urlImagem: '',
      categoriaRestaurante: 'COMIDA_REGIONAL',
    } as const;

    await atualizarRestaurante(7, dados);
    await atualizarStatusRestaurante(7, 'FECHADO');
    await substituirHorarios(7, []);

    expect(fetchFalso.mock.calls.map(([url]) => url)).toEqual([
      `${ORIGEM}/api/restaurantes/7`,
      `${ORIGEM}/api/restaurantes/7/status`,
      `${ORIGEM}/api/restaurantes/7/horarios`,
    ]);
    for (const [, init] of fetchFalso.mock.calls) {
      expect(init?.headers).toMatchObject({ Authorization: 'Bearer token-loja' });
      expect(String(init?.body)).not.toContain('ownerUserId');
    }
  });
});

describe('chavesRestaurantes', () => {
  it('aninha detalhe e horários sob a chave da listagem', () => {
    expect(chavesRestaurantes.detalhe(7)).toEqual(['restaurantes', 7]);
    expect(chavesRestaurantes.horarios(7)).toEqual(['restaurantes', 7, 'horarios']);
  });
});
