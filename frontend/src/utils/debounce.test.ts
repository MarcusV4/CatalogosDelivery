import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { criarDebounce } from './debounce';

const ATRASO = 400;

describe('criarDebounce', () => {
  const acao = vi.fn<(valor: string) => void>();

  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    acao.mockReset();
  });

  it('só executa depois do atraso, uma vez, com o último valor', () => {
    const agendador = criarDebounce(acao, ATRASO);

    agendador.agendar('p');
    vi.advanceTimersByTime(ATRASO - 1);
    agendador.agendar('pi');
    vi.advanceTimersByTime(ATRASO - 1);
    agendador.agendar('piz');

    expect(acao).not.toHaveBeenCalled();
    vi.advanceTimersByTime(ATRASO);
    expect(acao).toHaveBeenCalledTimes(1);
    expect(acao).toHaveBeenCalledWith('piz');
  });

  it('cancelar descarta o agendamento pendente', () => {
    const agendador = criarDebounce(acao, ATRASO);

    agendador.agendar('pizza');
    agendador.cancelar();
    vi.advanceTimersByTime(ATRASO * 2);

    expect(acao).not.toHaveBeenCalled();
  });

  it('executarAgora roda o pendente na hora e não roda de novo depois', () => {
    const agendador = criarDebounce(acao, ATRASO);

    agendador.agendar('açaí');
    agendador.executarAgora();
    vi.advanceTimersByTime(ATRASO * 2);

    expect(acao).toHaveBeenCalledTimes(1);
    expect(acao).toHaveBeenCalledWith('açaí');
  });

  it('executarAgora sem pendência não faz nada', () => {
    const agendador = criarDebounce(acao, ATRASO);

    agendador.executarAgora();
    agendador.agendar('x');
    vi.advanceTimersByTime(ATRASO);
    agendador.executarAgora();

    expect(acao).toHaveBeenCalledTimes(1);
  });

  it('volta a funcionar depois de executar', () => {
    const agendador = criarDebounce(acao, ATRASO);

    agendador.agendar('a');
    vi.advanceTimersByTime(ATRASO);
    agendador.agendar('b');
    vi.advanceTimersByTime(ATRASO);

    expect(acao.mock.calls).toEqual([['a'], ['b']]);
  });
});
