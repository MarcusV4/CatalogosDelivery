// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import { RedirecionarSessaoExpirada } from './RedirecionarSessaoExpirada';

function LoginComDestino() {
  const location = useLocation();
  const returnTo = (location.state as { returnTo?: string } | null)?.returnTo;
  return <p>Login para: {returnTo}</p>;
}

afterEach(cleanup);

describe('RedirecionarSessaoExpirada', () => {
  it('volta ao login e preserva a rota após uma resposta 401', () => {
    render(
      <MemoryRouter initialEntries={['/painel/restaurantes/1']}>
        <RedirecionarSessaoExpirada />
        <Routes>
          <Route path="/painel/restaurantes/1" element={<p>Painel</p>} />
          <Route path="/login" element={<LoginComDestino />} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent(window, new Event('auth:unauthorized'));

    expect(screen.getByText('Login para: /painel/restaurantes/1')).toBeTruthy();
    expect(screen.queryByText('Painel')).toBeNull();
  });

  it('também volta ao login quando a renovação do refresh token falha', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <RedirecionarSessaoExpirada />
        <Routes>
          <Route path="/" element={<p>Home</p>} />
          <Route path="/login" element={<LoginComDestino />} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent(window, new Event('auth:session-expired'));

    expect(screen.getByText('Login para: /')).toBeTruthy();
  });
});
