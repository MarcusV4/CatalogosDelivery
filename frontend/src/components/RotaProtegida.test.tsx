// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { SessaoOAuth } from '../services/oauth';
import { RotaProtegida } from './RotaProtegida';

const mocks = vi.hoisted(() => ({ sessao: null as SessaoOAuth | null }));

vi.mock('../hooks/useAutenticacao', () => ({
  useAutenticacao: () => ({ sessao: mocks.sessao }),
}));

function montarRotaProtegida() {
  return render(
    <MemoryRouter initialEntries={['/painel/restaurantes/1']}>
      <Routes>
        <Route path="/login" element={<p>Página de login</p>} />
        <Route element={<RotaProtegida />}>
          <Route path="/painel/restaurantes/:id" element={<p>Painel do restaurante</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  mocks.sessao = null;
});

afterEach(cleanup);

describe('RotaProtegida', () => {
  it('redireciona para login quando não existe sessão', () => {
    montarRotaProtegida();

    expect(screen.getByText('Página de login')).toBeTruthy();
    expect(screen.queryByText('Painel do restaurante')).toBeNull();
  });

  it('permite que uma sessão LOJA acesse o painel', () => {
    mocks.sessao = {
      accessToken: 'access',
      refreshToken: 'refresh',
      tipo: 'LOJA',
      usuarioId: 'loja-1',
      expiraEm: Date.now() + 60_000,
    };
    montarRotaProtegida();

    expect(screen.getByText('Painel do restaurante')).toBeTruthy();
  });

  it('permite o papel LOJISTA usado pelo back-end do Catálogo', () => {
    mocks.sessao = {
      accessToken: 'access',
      refreshToken: 'refresh',
      tipo: 'LOJISTA',
      usuarioId: '57',
      expiraEm: Date.now() + 60_000,
    };
    montarRotaProtegida();

    expect(screen.getByText('Painel do restaurante')).toBeTruthy();
  });

  it('nega a rota de proprietário para uma sessão CLIENTE', () => {
    mocks.sessao = {
      accessToken: 'access',
      refreshToken: 'refresh',
      tipo: 'CLIENTE',
      usuarioId: 'cliente-1',
      expiraEm: Date.now() + 60_000,
    };
    montarRotaProtegida();

    expect(screen.getByRole('alert').textContent).toContain('somente para contas de loja');
    expect(screen.queryByText('Painel do restaurante')).toBeNull();
  });
});
