// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ContextoAutenticacao } from '../../context/autenticacaoContexto';
import { NotificacaoProvider } from '../../context/NotificacaoProvider';
import { criarQueryClient } from '../../services/consultas';
import { definirSessaoAtual, limparSessaoAtual } from '../../services/oauth';
import type { Restaurante } from '../../services/restaurantes';
import { PainelRestaurantes } from './index';

const ORIGEM = 'http://localhost:8004';

function restaurante(id: number, dono: number, nome: string): Restaurante {
  return {
    restauranteId: id,
    restauranteNome: nome,
    restauranteDesc: `Descrição de ${nome}`,
    logradouro: 'Rua A',
    numero: '10',
    complemento: null,
    bairro: 'Centro',
    cidade: 'Recife',
    estado: 'PE',
    cep: '50000-000',
    restauranteTelefone: '81999990000',
    urlImagem: null,
    ownerUserId: dono,
    statusRestaurante: 'ABERTO',
    categoriaRestaurante: 'PIZZARIA',
    taxaEntrega: 500,
    tempoEstimadoMin: 30,
    tempoEstimadoMax: 50,
    abertoAgora: true,
  };
}

function resposta(dados: unknown, status = 200): Response {
  return new Response(JSON.stringify(dados), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('PainelRestaurantes', () => {
  const fetchFalso = vi.fn<typeof fetch>();
  let restaurantesServidor: Restaurante[];
  let negarStatus: boolean;
  let falharCadastro: boolean;
  let falharListagem: boolean;
  let horariosServidor: Array<{ diaSemana: string; horaAbertura: string; horaFechamento: string }>;

  beforeEach(() => {
    vi.stubEnv('VITE_API_RESTAURANTES_URL', ORIGEM);
    vi.stubGlobal('fetch', fetchFalso);
    definirSessaoAtual({
      accessToken: 'token-loja',
      refreshToken: 'refresh',
      tipo: 'LOJA',
      usuarioId: '57',
      expiraEm: Date.now() + 60_000,
    });
    restaurantesServidor = [
      restaurante(1, 57, 'Cantina da Praça'),
      restaurante(2, 99, 'Loja de outra pessoa'),
    ];
    negarStatus = false;
    falharCadastro = false;
    falharListagem = false;
    horariosServidor = [
      { diaSemana: 'SEGUNDA', horaAbertura: '09:00:00', horaFechamento: '18:00:00' },
    ];
    fetchFalso.mockImplementation(async (input, init) => {
      const url = String(input);
      const metodo = init?.method ?? 'GET';
      if (url === `${ORIGEM}/api/restaurantes` && metodo === 'GET') {
        if (falharListagem) {
          return resposta({ erro: true, mensagem: 'Serviço indisponível.' }, 503);
        }
        return resposta(restaurantesServidor);
      }
      if (url === `${ORIGEM}/api/restaurantes` && metodo === 'POST') {
        if (falharCadastro) return resposta({ erro: true, mensagem: 'CEP inválido.' }, 400);
        const dados = JSON.parse(String(init?.body)) as Omit<
          Restaurante,
          'restauranteId' | 'ownerUserId' | 'statusRestaurante' | 'abertoAgora'
        >;
        const novo = { ...restaurante(3, 57, dados.restauranteNome), ...dados };
        restaurantesServidor = [...restaurantesServidor, novo];
        return resposta(novo, 201);
      }
      if (url === `${ORIGEM}/api/restaurantes/1` && metodo === 'PUT') {
        const dados = JSON.parse(String(init?.body)) as Partial<Restaurante>;
        restaurantesServidor = restaurantesServidor.map((r) =>
          r.restauranteId === 1 ? { ...r, ...dados } : r,
        );
        return resposta(restaurantesServidor[0]);
      }
      if (url === `${ORIGEM}/api/restaurantes/1/status` && metodo === 'PATCH') {
        if (negarStatus)
          return resposta(
            { erro: true, mensagem: 'Usuário não é proprietário do restaurante.' },
            403,
          );
        const body = JSON.parse(String(init?.body)) as {
          statusRestaurante: Restaurante['statusRestaurante'];
        };
        restaurantesServidor = restaurantesServidor.map((r) =>
          r.restauranteId === 1 ? { ...r, statusRestaurante: body.statusRestaurante } : r,
        );
        return resposta(restaurantesServidor[0]);
      }
      if (url === `${ORIGEM}/api/restaurantes/1/horarios` && metodo === 'GET')
        return resposta(horariosServidor);
      if (url === `${ORIGEM}/api/restaurantes/1/horarios` && metodo === 'PUT') {
        horariosServidor = (JSON.parse(String(init?.body)) as { horarios: typeof horariosServidor })
          .horarios;
        return resposta(horariosServidor);
      }
      return resposta({ erro: true, mensagem: 'Rota inesperada.' }, 404);
    });
  });

  afterEach(() => {
    cleanup();
    limparSessaoAtual();
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    fetchFalso.mockReset();
  });

  function abrirPainel() {
    const cliente = criarQueryClient(vi.fn());
    render(
      <ContextoAutenticacao.Provider
        value={{
          sessao: {
            accessToken: 'token-loja',
            refreshToken: 'refresh',
            tipo: 'LOJA',
            usuarioId: '57',
            expiraEm: Date.now() + 60_000,
          },
          erro: null,
          entrar: vi.fn(async () => {}),
          concluirCallback: vi.fn(async () => '/'),
          sair: vi.fn(async () => {}),
          limparErro: vi.fn(),
        }}
      >
        <NotificacaoProvider>
          <QueryClientProvider client={cliente}>
            <MemoryRouter initialEntries={['/painel/restaurantes']}>
              <Routes>
                <Route path="/painel/restaurantes" element={<PainelRestaurantes />} />
              </Routes>
            </MemoryRouter>
          </QueryClientProvider>
        </NotificacaoProvider>
      </ContextoAutenticacao.Provider>,
    );
  }

  it('lista só os restaurantes do usuário e autentica a consulta', async () => {
    abrirPainel();
    expect(await screen.findByRole('heading', { name: 'Cantina da Praça' })).toBeTruthy();
    expect(screen.queryByText('Loja de outra pessoa')).toBeNull();
    expect(fetchFalso.mock.calls[0]?.[1]?.headers).toMatchObject({
      Authorization: 'Bearer token-loja',
    });
  });

  it('mostra estado vazio quando a conta ainda não tem restaurantes', async () => {
    restaurantesServidor = [];
    abrirPainel();
    expect(await screen.findByText('Você ainda não tem restaurantes cadastrados.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Cadastrar restaurante' })).toBeTruthy();
  });

  it('mostra erro da API e permite repetir a listagem', async () => {
    falharListagem = true;
    abrirPainel();
    expect(await screen.findByText('Serviço indisponível.', {}, { timeout: 5_000 })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Tentar de novo' })).toBeTruthy();
  });

  it('cadastra restaurante enviando dados e token sem ownerUserId', async () => {
    const usuario = userEvent.setup();
    abrirPainel();
    await screen.findByRole('heading', { name: 'Cantina da Praça' });
    await usuario.click(screen.getByRole('button', { name: 'Novo restaurante' }));
    const valores: Record<string, string> = {
      'restaurante-nome': 'Bistrô do Recife',
      'restaurante-descricao': 'Pratos regionais',
      'restaurante-telefone': '81999990000',
      'restaurante-logradouro': 'Rua Nova',
      'restaurante-numero': '25',
      'restaurante-bairro': 'Boa Vista',
      'restaurante-cidade': 'Recife',
      'restaurante-estado': 'PE',
      'restaurante-cep': '50000-000',
      'restaurante-taxa': '4,50',
      'restaurante-tempo-min': '20',
      'restaurante-tempo-max': '40',
    };
    for (const [id, valor] of Object.entries(valores)) {
      await usuario.type(document.getElementById(id) as HTMLElement, valor);
    }
    await usuario.selectOptions(screen.getByLabelText('Categoria'), 'COMIDA_REGIONAL');
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar restaurante' }));

    expect(await screen.findByRole('heading', { name: 'Bistrô do Recife' })).toBeTruthy();
    const chamada = fetchFalso.mock.calls.find(([, init]) => init?.method === 'POST');
    expect(chamada?.[1]?.headers).toMatchObject({ Authorization: 'Bearer token-loja' });
    expect(JSON.parse(String(chamada?.[1]?.body))).toMatchObject({
      restauranteNome: 'Bistrô do Recife',
      estado: 'PE',
      taxaEntrega: 450,
      categoriaRestaurante: 'COMIDA_REGIONAL',
    });
    expect(JSON.parse(String(chamada?.[1]?.body))).not.toHaveProperty('ownerUserId');
  });

  it('mostra a validação 400 da API no formulário de cadastro', async () => {
    falharCadastro = true;
    const usuario = userEvent.setup();
    abrirPainel();
    await screen.findByRole('heading', { name: 'Cantina da Praça' });
    await usuario.click(screen.getByRole('button', { name: 'Novo restaurante' }));
    const valores: Record<string, string> = {
      'restaurante-nome': 'Bistrô do Recife',
      'restaurante-descricao': 'Pratos regionais',
      'restaurante-telefone': '81999990000',
      'restaurante-logradouro': 'Rua Nova',
      'restaurante-numero': '25',
      'restaurante-bairro': 'Boa Vista',
      'restaurante-cidade': 'Recife',
      'restaurante-estado': 'PE',
      'restaurante-cep': '50000-000',
      'restaurante-taxa': '4,50',
      'restaurante-tempo-min': '20',
      'restaurante-tempo-max': '40',
    };
    for (const [id, valor] of Object.entries(valores)) {
      await usuario.type(document.getElementById(id) as HTMLElement, valor);
    }
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar restaurante' }));
    expect((await screen.findByRole('alert')).textContent).toContain('CEP inválido.');
  });

  it('edita os dados, converte taxa de centavos para reais e envia atualização autenticada', async () => {
    const usuario = userEvent.setup();
    abrirPainel();
    await screen.findByRole('heading', { name: 'Cantina da Praça' });
    await usuario.click(screen.getByRole('button', { name: 'Editar dados' }));
    expect(screen.getByLabelText('Taxa de entrega (R$)')).toHaveProperty('value', '5');
    const campoNome = screen.getByLabelText('Nome');
    await usuario.clear(campoNome);
    await usuario.type(campoNome, 'Cantina Atualizada');
    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(await screen.findByRole('heading', { name: 'Cantina Atualizada' })).toBeTruthy();
    const chamada = fetchFalso.mock.calls.find(([, init]) => init?.method === 'PUT');
    expect(chamada?.[1]?.headers).toMatchObject({ Authorization: 'Bearer token-loja' });
    expect(JSON.parse(String(chamada?.[1]?.body))).toMatchObject({
      restauranteNome: 'Cantina Atualizada',
      taxaEntrega: 500,
    });
    expect(JSON.parse(String(chamada?.[1]?.body))).not.toHaveProperty('ownerUserId');
  });

  it('envia a alteração de status autenticada e exibe a mensagem 403 da API', async () => {
    negarStatus = true;
    const usuario = userEvent.setup();
    abrirPainel();
    await screen.findByRole('heading', { name: 'Cantina da Praça' });
    await usuario.selectOptions(screen.getByLabelText('Status'), 'FECHADO');

    expect((await screen.findByRole('alert')).textContent).toContain('não é proprietário');
    const chamada = fetchFalso.mock.calls.find(([, init]) => init?.method === 'PATCH');
    expect(chamada?.[1]?.headers).toMatchObject({ Authorization: 'Bearer token-loja' });
    expect(JSON.parse(String(chamada?.[1]?.body))).toEqual({ statusRestaurante: 'FECHADO' });
  });

  it('carrega, edita e salva os horários usando token e corpo conforme o contrato', async () => {
    const usuario = userEvent.setup();
    abrirPainel();
    await screen.findByRole('heading', { name: 'Cantina da Praça' });
    await usuario.click(screen.getByRole('button', { name: 'Editar horários' }));
    await screen.findByRole('heading', { name: 'Horários de funcionamento' });
    await usuario.clear(screen.getByLabelText('Abre'));
    await usuario.type(screen.getByLabelText('Abre'), '08:00');
    await usuario.click(screen.getByRole('button', { name: 'Salvar horários' }));

    await waitFor(() => expect(horariosServidor[0]?.horaAbertura).toBe('08:00:00'));
    const chamada = fetchFalso.mock.calls.find(([, init]) => init?.method === 'PUT');
    expect(chamada?.[1]?.headers).toMatchObject({ Authorization: 'Bearer token-loja' });
    expect(JSON.parse(String(chamada?.[1]?.body))).toEqual({
      horarios: [{ diaSemana: 'SEGUNDA', horaAbertura: '08:00:00', horaFechamento: '18:00:00' }],
    });
  });
});
