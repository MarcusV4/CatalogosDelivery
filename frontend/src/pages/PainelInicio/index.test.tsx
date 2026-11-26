// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { criarQueryClient } from '../../services/consultas';
import type { SessaoOAuth } from '../../services/oauth';
import { PainelInicio } from './index';

const ORIGEM = 'http://localhost:8004';
const mocks = vi.hoisted(() => ({ sessao: null as SessaoOAuth | null }));

vi.mock('../../hooks/useAutenticacao', () => ({
  useAutenticacao: () => ({ sessao: mocks.sessao }),
}));

function restaurante(restauranteId: number, restauranteNome: string, ownerUserId: number) {
  return {
    restauranteId,
    restauranteNome,
    restauranteDesc: null,
    logradouro: null,
    numero: null,
    complemento: null,
    bairro: null,
    cidade: 'Recife',
    estado: 'PE',
    cep: null,
    restauranteTelefone: null,
    urlImagem: null,
    ownerUserId,
    statusRestaurante: 'ABERTO',
    categoriaRestaurante: 'PIZZARIA',
    taxaEntrega: 0,
    tempoEstimadoMin: null,
    tempoEstimadoMax: null,
    abertoAgora: true,
  };
}

function sessaoDeLoja(usuarioId: string): SessaoOAuth {
  return {
    accessToken: 'access',
    refreshToken: 'refresh',
    tipo: 'LOJA',
    usuarioId,
    expiraEm: Date.now() + 60_000,
  };
}

function resposta(dados: unknown, status = 200): Response {
  return new Response(JSON.stringify(dados), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('PainelInicio', () => {
  const fetchFalso = vi.fn<typeof fetch>();

  beforeEach(() => {
    vi.stubEnv('VITE_API_RESTAURANTES_URL', ORIGEM);
    vi.stubGlobal('fetch', fetchFalso);
    mocks.sessao = sessaoDeLoja('7');
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    fetchFalso.mockReset();
  });

  function abrir() {
    render(
      <QueryClientProvider client={criarQueryClient(vi.fn())}>
        <MemoryRouter>
          <PainelInicio />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  }

  it('lista só os restaurantes do dono logado, com o link para o cardápio', async () => {
    fetchFalso.mockResolvedValue(
      resposta([restaurante(3, 'Pizzaria do Dono', 7), restaurante(4, 'Restaurante de Outro', 8)]),
    );
    abrir();

    expect(await screen.findByText('Pizzaria do Dono')).toBeTruthy();
    expect(screen.queryByText('Restaurante de Outro')).toBeNull();
    expect(screen.getByRole('link', { name: 'Gerenciar cardápio' }).getAttribute('href')).toBe(
      '/painel/restaurantes/3/cardapio',
    );
    expect(String(fetchFalso.mock.calls[0]?.[0])).toBe(`${ORIGEM}/api/restaurantes`);
  });

  it('mostra estado vazio quando o dono não tem restaurante ativo', async () => {
    fetchFalso.mockResolvedValue(resposta([restaurante(4, 'Restaurante de Outro', 8)]));
    abrir();

    expect(
      await screen.findByText('Nenhum restaurante ativo está vinculado à sua conta.'),
    ).toBeTruthy();
  });

  it('não lista nada quando o id do usuário no token não é numérico', async () => {
    mocks.sessao = sessaoDeLoja('loja-1');
    fetchFalso.mockResolvedValue(resposta([restaurante(3, 'Pizzaria do Dono', 7)]));
    abrir();

    expect(
      await screen.findByText('Nenhum restaurante ativo está vinculado à sua conta.'),
    ).toBeTruthy();
  });

  it('mostra erro e permite tentar de novo quando o serviço falha', async () => {
    fetchFalso
      .mockResolvedValueOnce(resposta({ erro: true, mensagem: 'Serviço instável.' }, 400))
      .mockResolvedValueOnce(resposta([restaurante(3, 'Pizzaria do Dono', 7)]));
    abrir();

    expect(await screen.findByText('Serviço instável.')).toBeTruthy();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Tentar de novo' }));
    expect(await screen.findByText('Pizzaria do Dono')).toBeTruthy();
  });
});
