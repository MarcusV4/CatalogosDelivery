import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, STATUS_SEM_RESPOSTA, requisitar } from './http';

const ORIGEM = 'http://localhost:8004';

function respostaJson(status: number, corpo: unknown): Response {
  return new Response(JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

async function capturarErro(promessa: Promise<unknown>): Promise<ApiError> {
  const erro = await promessa.catch((e: unknown) => e);
  expect(erro).toBeInstanceOf(ApiError);
  return erro as ApiError;
}

describe('requisitar', () => {
  const fetchFalso = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubEnv('VITE_API_RESTAURANTES_URL', `${ORIGEM}/`);
    vi.stubGlobal('fetch', fetchFalso);
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    fetchFalso.mockReset();
  });

  it('monta a URL a partir da origem do serviço e lê o JSON de sucesso', async () => {
    fetchFalso.mockResolvedValue(respostaJson(200, [{ restauranteId: 1 }]));

    await expect(requisitar('restaurantes', '/api/restaurantes')).resolves.toEqual([
      { restauranteId: 1 },
    ]);
    const [url, init] = fetchFalso.mock.calls[0] ?? [];
    expect(url).toBe(`${ORIGEM}/api/restaurantes`);
    expect(init?.method).toBe('GET');
    expect(init?.body).toBeUndefined();
  });

  it('envia o corpo como JSON', async () => {
    fetchFalso.mockResolvedValue(respostaJson(201, { restauranteId: 2 }));

    await requisitar('restaurantes', '/api/restaurantes', { metodo: 'POST', corpo: { nome: 'A' } });

    const init = fetchFalso.mock.calls[0]?.[1];
    expect(init?.body).toBe('{"nome":"A"}');
    expect(init?.headers).toMatchObject({ 'Content-Type': 'application/json' });
  });

  it('resolve undefined em respostas 204', async () => {
    fetchFalso.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(
      requisitar('restaurantes', '/api/restaurantes/1', { metodo: 'DELETE' }),
    ).resolves.toBeUndefined();
  });

  it('converte o erro padrão em ApiError com status e mensagem', async () => {
    fetchFalso.mockResolvedValue(
      respostaJson(404, { erro: true, mensagem: 'Restaurante 99 não encontrado.' }),
    );

    const erro = await capturarErro(requisitar('restaurantes', '/api/restaurantes/99'));

    expect(erro.status).toBe(404);
    expect(erro.message).toBe('Restaurante 99 não encontrado.');
    expect(erro.ehErroDoCliente).toBe(true);
  });

  it('mantém a mensagem do erro padrão em respostas 5xx', async () => {
    fetchFalso.mockResolvedValue(
      respostaJson(503, { erro: true, mensagem: 'O serviço de Cardápio está indisponível.' }),
    );

    const erro = await capturarErro(requisitar('restaurantes', '/api/restaurantes'));

    expect(erro.status).toBe(503);
    expect(erro.message).toBe('O serviço de Cardápio está indisponível.');
    expect(erro.ehErroDoCliente).toBe(false);
  });

  it('usa uma mensagem em português quando o 5xx não vem no formato padrão', async () => {
    fetchFalso.mockResolvedValue(new Response('Bad Gateway', { status: 502 }));

    const erro = await capturarErro(requisitar('restaurantes', '/api/restaurantes'));

    expect(erro.status).toBe(502);
    expect(erro.message).toBe(
      'O serviço de Restaurantes está indisponível no momento. Tente novamente em instantes.',
    );
  });

  it('converte falha de rede em ApiError com status 0', async () => {
    fetchFalso.mockRejectedValue(new TypeError('Failed to fetch'));

    const erro = await capturarErro(requisitar('restaurantes', '/api/restaurantes'));

    expect(erro.status).toBe(STATUS_SEM_RESPOSTA);
    expect(erro.message).toBe(
      'Não foi possível conectar ao serviço de Restaurantes. Verifique sua conexão ou tente mais tarde.',
    );
  });

  it('converte timeout em ApiError com status 0', async () => {
    fetchFalso.mockImplementation(
      (_url, init) =>
        new Promise((_resolver, rejeitar) => {
          init?.signal?.addEventListener('abort', () => rejeitar(init.signal?.reason));
        }),
    );

    const erro = await capturarErro(
      requisitar('restaurantes', '/api/restaurantes', { timeoutMs: 10 }),
    );

    expect(erro.status).toBe(STATUS_SEM_RESPOSTA);
    expect(erro.message).toBe(
      'O serviço de Restaurantes demorou demais para responder. Tente novamente.',
    );
  });

  it('repassa o cancelamento feito por quem chamou, sem transformar em ApiError', async () => {
    const controlador = new AbortController();
    fetchFalso.mockImplementation(
      (_url, init) =>
        new Promise((_resolver, rejeitar) => {
          init?.signal?.addEventListener('abort', () => rejeitar(init.signal?.reason));
        }),
    );

    const promessa = requisitar('restaurantes', '/api/restaurantes', {
      signal: controlador.signal,
    });
    controlador.abort();

    await expect(promessa).rejects.not.toBeInstanceOf(ApiError);
  });

  it('acusa a variável de ambiente ausente', async () => {
    vi.stubEnv('VITE_API_RESTAURANTES_URL', '');

    await expect(requisitar('restaurantes', '/api/restaurantes')).rejects.toThrow(
      'VITE_API_RESTAURANTES_URL',
    );
  });
});
