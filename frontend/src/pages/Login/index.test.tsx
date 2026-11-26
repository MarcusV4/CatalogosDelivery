// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Login, RetornoOAuth } from './index';

const mocks = vi.hoisted(() => ({
  entrar: vi.fn(),
  sair: vi.fn(),
  concluirCallback: vi.fn(),
  limparErro: vi.fn(),
  sessao: null as null | { tipo: 'LOJA' | 'CLIENTE' },
  erro: null as string | null,
}));

vi.mock('../../hooks/useAutenticacao', () => ({
  useAutenticacao: () => ({
    entrar: mocks.entrar,
    sair: mocks.sair,
    concluirCallback: mocks.concluirCallback,
    limparErro: mocks.limparErro,
    sessao: mocks.sessao,
    erro: mocks.erro,
  }),
}));

afterEach(() => {
  cleanup();
  mocks.entrar.mockReset();
  mocks.concluirCallback.mockReset();
  mocks.sair.mockReset();
  mocks.limparErro.mockReset();
  mocks.sessao = null;
  mocks.erro = null;
});

describe('Login', () => {
  it('inicia o login hospedado e não coleta senha no frontend', () => {
    render(
      <MemoryRouter initialEntries={[{ pathname: '/login', state: { returnTo: '/painel' } }]}>
        <Login />
      </MemoryRouter>,
    );

    expect(screen.queryByLabelText(/senha/i)).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Entrar com sua conta' }));

    expect(mocks.entrar).toHaveBeenCalledWith('/painel');
  });

  it('exibe uma falha amigável quando o serviço de autenticação está indisponível', () => {
    mocks.erro = 'O serviço de autenticação está indisponível no momento.';
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    expect(screen.getByRole('alert').textContent).toContain('indisponível');
  });

  it('processa o callback e segue para a rota local originalmente solicitada', async () => {
    mocks.concluirCallback.mockResolvedValue('/painel/restaurantes/1');
    render(
      <MemoryRouter initialEntries={['/oauth/callback?code=abc&state=ok']}>
        <Routes>
          <Route path="/oauth/callback" element={<RetornoOAuth />} />
          <Route path="/painel/restaurantes/1" element={<p>Painel</p>} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText('Painel')).toBeTruthy());
    expect(mocks.concluirCallback).toHaveBeenCalledWith('?code=abc&state=ok');
  });
});
