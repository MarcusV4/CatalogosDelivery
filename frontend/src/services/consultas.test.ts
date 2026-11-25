import { describe, expect, it, vi } from 'vitest';
import { criarQueryClient, deveTentarDeNovo, mensagemDoErro } from './consultas';
import { ApiError } from './http';

describe('deveTentarDeNovo', () => {
  it('não repete erros 4xx', () => {
    expect(deveTentarDeNovo(0, new ApiError(404, 'Não encontrado'))).toBe(false);
    expect(deveTentarDeNovo(0, new ApiError(400, 'Inválido'))).toBe(false);
  });

  it('repete falhas de rede e 5xx até duas vezes', () => {
    expect(deveTentarDeNovo(0, new ApiError(0, 'Sem conexão'))).toBe(true);
    expect(deveTentarDeNovo(1, new ApiError(503, 'Fora do ar'))).toBe(true);
    expect(deveTentarDeNovo(2, new ApiError(503, 'Fora do ar'))).toBe(false);
  });
});

describe('mensagemDoErro', () => {
  it('usa a mensagem do ApiError e esconde detalhes de outros erros', () => {
    expect(mensagemDoErro(new ApiError(404, 'Restaurante não encontrado.'))).toBe(
      'Restaurante não encontrado.',
    );
    expect(mensagemDoErro(new Error('stack interna'))).toBe(
      'Ocorreu um erro inesperado. Tente novamente.',
    );
  });
});

describe('criarQueryClient', () => {
  const falhar = () => Promise.reject(new ApiError(404, 'Restaurante não encontrado.'));

  it('notifica os erros das consultas', async () => {
    const notificar = vi.fn();
    const cliente = criarQueryClient(notificar);

    await cliente.fetchQuery({ queryKey: ['a'], queryFn: falhar }).catch(() => undefined);

    expect(notificar).toHaveBeenCalledWith('Restaurante não encontrado.');
  });

  it('não notifica quando a consulta pede silenciarErro', async () => {
    const notificar = vi.fn();
    const cliente = criarQueryClient(notificar);

    await cliente
      .fetchQuery({ queryKey: ['b'], queryFn: falhar, meta: { silenciarErro: true } })
      .catch(() => undefined);

    expect(notificar).not.toHaveBeenCalled();
  });

  it('notifica os erros das mutações', async () => {
    const notificar = vi.fn();
    const cliente = criarQueryClient(notificar);

    await cliente
      .getMutationCache()
      .build(cliente, { mutationFn: falhar })
      .execute(undefined)
      .catch(() => undefined);

    expect(notificar).toHaveBeenCalledWith('Restaurante não encontrado.');
  });
});
