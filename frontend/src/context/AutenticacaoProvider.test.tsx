// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AutenticacaoProvider } from './AutenticacaoProvider';
import { useAutenticacao } from '../hooks/useAutenticacao';
import { limparSessaoAtual, type SessaoOAuth } from '../services/oauth';

const mocks = vi.hoisted(() => ({
  iniciarFluxoLogin: vi.fn(),
  concluirFluxoLogin: vi.fn(),
  renovarTokens: vi.fn(),
  revogarRefreshToken: vi.fn(),
  configuracaoOAuth: vi.fn(() => ({
    emissor: 'http://localhost:18001',
    clientId: 'catalogo-web',
    redirectUri: 'http://localhost:5173/oauth/callback',
    escopos: '',
  })),
  metadados: vi.fn(async () => ({
    issuer: 'http://localhost:18001',
    authorization_endpoint: 'http://localhost:18001/v1/oauth/autorizar',
    token_endpoint: 'http://localhost:18001/v1/oauth/tokens',
    revocation_endpoint: 'http://localhost:18001/v1/oauth/revogar',
  })),
}));

vi.mock('../services/oauthFlow', () => ({
  iniciarFluxoLogin: mocks.iniciarFluxoLogin,
  concluirFluxoLogin: mocks.concluirFluxoLogin,
}));

vi.mock('../services/oauth', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/oauth')>()),
  configuracaoOAuth: mocks.configuracaoOAuth,
  obterMetadadosOAuth: mocks.metadados,
  renovarTokens: mocks.renovarTokens,
  revogarRefreshToken: mocks.revogarRefreshToken,
}));

const sessaoBase: SessaoOAuth = {
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  tipo: 'LOJA',
  usuarioId: 'user-1',
  expiraEm: Date.now() + 5 * 60_000,
};

function ConsumidorAutenticacao() {
  const { sessao, erro, entrar, concluirCallback, sair } = useAutenticacao();
  return (
    <main>
      <output aria-label="Papel">{sessao?.tipo ?? 'sem sessão'}</output>
      <output aria-label="Token">{sessao?.accessToken ?? 'sem token'}</output>
      {erro ? <p role="alert">{erro}</p> : null}
      <button onClick={() => void entrar('/painel')}>Entrar</button>
      <button onClick={() => void concluirCallback('?code=abc&state=ok')}>Concluir callback</button>
      <button onClick={() => void sair()}>Sair</button>
    </main>
  );
}

function renderProvider() {
  return render(
    <AutenticacaoProvider>
      <ConsumidorAutenticacao />
    </AutenticacaoProvider>,
  );
}

afterEach(() => {
  cleanup();
  limparSessaoAtual();
  vi.useRealTimers();
  vi.clearAllMocks();
});

describe('AutenticacaoProvider', () => {
  it('inicia o fluxo hospedado sem coletar credenciais no frontend', async () => {
    renderProvider();
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(mocks.iniciarFluxoLogin).toHaveBeenCalledWith('/painel'));
    expect(screen.queryByLabelText(/senha/i)).toBeNull();
  });

  it('estabelece a sessão depois de processar o callback OAuth', async () => {
    mocks.concluirFluxoLogin.mockResolvedValue({ sessao: sessaoBase, returnTo: '/painel' });
    renderProvider();
    fireEvent.click(screen.getByRole('button', { name: 'Concluir callback' }));

    await waitFor(() => expect(screen.getByLabelText('Papel').textContent).toBe('LOJA'));
    expect(screen.getByLabelText('Token').textContent).toBe('access-1');
    expect(mocks.concluirFluxoLogin).toHaveBeenCalledWith('?code=abc&state=ok');
  });

  it('renova a sessão antes do vencimento e guarda o refresh token rotacionado', async () => {
    vi.useFakeTimers();
    const sessaoQuaseExpirada = { ...sessaoBase, expiraEm: Date.now() + 65_000 };
    const sessaoRenovada = {
      ...sessaoBase,
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
      expiraEm: Date.now() + 1_500_000,
    };
    mocks.concluirFluxoLogin.mockResolvedValue({ sessao: sessaoQuaseExpirada, returnTo: '/' });
    mocks.renovarTokens.mockResolvedValue(sessaoRenovada);
    renderProvider();
    fireEvent.click(screen.getByRole('button', { name: 'Concluir callback' }));
    await act(async () => Promise.resolve());

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5_001);
    });

    expect(mocks.renovarTokens).toHaveBeenCalledWith(
      expect.anything(),
      expect.anything(),
      'refresh-1',
    );
    expect(screen.getByLabelText('Token').textContent).toBe('access-2');
  });

  it('revoga a sessão no logout e remove os tokens em memória', async () => {
    mocks.concluirFluxoLogin.mockResolvedValue({ sessao: sessaoBase, returnTo: '/' });
    mocks.revogarRefreshToken.mockResolvedValue(undefined);
    renderProvider();
    fireEvent.click(screen.getByRole('button', { name: 'Concluir callback' }));
    await waitFor(() => expect(screen.getByLabelText('Token').textContent).toBe('access-1'));

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));

    await waitFor(() => expect(screen.getByLabelText('Token').textContent).toBe('sem token'));
    expect(mocks.revogarRefreshToken).toHaveBeenCalledWith(
      expect.anything(),
      expect.anything(),
      'refresh-1',
    );
  });

  it('limpa a sessão local e informa quando o serviço falha no logout', async () => {
    mocks.concluirFluxoLogin.mockResolvedValue({ sessao: sessaoBase, returnTo: '/' });
    mocks.revogarRefreshToken.mockRejectedValue(new Error('Identidade indisponível'));
    renderProvider();
    fireEvent.click(screen.getByRole('button', { name: 'Concluir callback' }));
    await waitFor(() => expect(screen.getByLabelText('Token').textContent).toBe('access-1'));

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));

    await waitFor(() => expect(screen.getByLabelText('Token').textContent).toBe('sem token'));
    expect(screen.getByRole('alert').textContent).toContain('Identidade indisponível');
  });

  it('encerra a sessão e solicita novo login se o refresh token expirar ou for recusado', async () => {
    vi.useFakeTimers();
    const sessaoQuaseExpirada = { ...sessaoBase, expiraEm: Date.now() + 20_000 };
    mocks.concluirFluxoLogin.mockResolvedValue({ sessao: sessaoQuaseExpirada, returnTo: '/' });
    mocks.renovarTokens.mockRejectedValue(new Error('Sessão revogada'));
    renderProvider();
    fireEvent.click(screen.getByRole('button', { name: 'Concluir callback' }));
    await act(async () => Promise.resolve());

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0);
    });

    expect(screen.getByLabelText('Token').textContent).toBe('sem token');
    expect(screen.getByRole('alert').textContent).toContain('Sua sessão expirou');
    expect(mocks.renovarTokens).toHaveBeenCalledWith(
      expect.anything(),
      expect.anything(),
      'refresh-1',
    );
  });

  it('encerra a sessão quando uma API protegida devolve 401', async () => {
    mocks.concluirFluxoLogin.mockResolvedValue({ sessao: sessaoBase, returnTo: '/' });
    renderProvider();
    fireEvent.click(screen.getByRole('button', { name: 'Concluir callback' }));
    await waitFor(() => expect(screen.getByLabelText('Token').textContent).toBe('access-1'));

    act(() => window.dispatchEvent(new Event('auth:unauthorized')));

    expect(screen.getByLabelText('Token').textContent).toBe('sem token');
  });
});
