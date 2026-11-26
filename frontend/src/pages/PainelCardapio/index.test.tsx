// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { NotificacaoProvider } from '../../context/NotificacaoProvider';
import type { Produto } from '../../services/cardapio';
import { criarQueryClient } from '../../services/consultas';
import { PainelCardapio } from './index';

const ORIGEM = 'http://localhost:8005';

function produtoBase(): Produto {
  return {
    id: 7,
    nome: 'Pizza Recife',
    descricao: 'Mussarela',
    preco: 2590,
    disponivel: true,
    categoria: 'PIZZA',
    restauranteId: 3,
    urlImagem: null,
    adicionais: [],
  };
}

function resposta(dados: unknown, status = 200): Response {
  return new Response(JSON.stringify(dados), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('PainelCardapio', () => {
  const fetchFalso = vi.fn<typeof fetch>();
  let produtosServidor: Produto[];
  let falharProximoCadastro: boolean;
  let falharProximaLeitura: boolean;

  beforeEach(() => {
    vi.stubEnv('VITE_API_CARDAPIO_URL', ORIGEM);
    vi.stubGlobal('fetch', fetchFalso);
    produtosServidor = [produtoBase()];
    falharProximoCadastro = false;
    falharProximaLeitura = false;
    fetchFalso.mockImplementation(async (input, init) => {
      const url = String(input);
      const metodo = init?.method ?? 'GET';

      if (url === `${ORIGEM}/api/categorias`) {
        return resposta(['PIZZA', 'BEBIDA']);
      }
      if (url === `${ORIGEM}/api/restaurantes/3/cardapio` && metodo === 'GET') {
        if (falharProximaLeitura) {
          falharProximaLeitura = false;
          return resposta(
            { erro: true, mensagem: 'Falha temporária ao carregar o cardápio.' },
            400,
          );
        }
        return resposta(produtosServidor);
      }
      if (url === `${ORIGEM}/api/restaurantes/3/cardapio` && metodo === 'POST') {
        if (falharProximoCadastro) {
          falharProximoCadastro = false;
          return resposta({ erro: true, mensagem: 'Preço inválido.' }, 400);
        }
        const produto = {
          ...JSON.parse(String(init?.body)),
          id: 8,
          restauranteId: 3,
          adicionais: [],
        };
        produtosServidor = [...produtosServidor, produto];
        return resposta(produto, 201);
      }
      if (url === `${ORIGEM}/api/produtos/7` && metodo === 'PUT') {
        produtosServidor = produtosServidor.map((produto) => ({
          ...produto,
          ...JSON.parse(String(init?.body)),
        }));
        return resposta(produtosServidor[0]);
      }
      if (url === `${ORIGEM}/api/produtos/7/disponibilidade` && metodo === 'PATCH') {
        const { disponivel } = JSON.parse(String(init?.body));
        produtosServidor = produtosServidor.map((produto) => ({ ...produto, disponivel }));
        return resposta(produtosServidor[0]);
      }
      if (url === `${ORIGEM}/api/produtos/7` && metodo === 'DELETE') {
        produtosServidor = produtosServidor.filter((produto) => produto.id !== 7);
        return new Response(null, { status: 204 });
      }
      return resposta({ erro: true, mensagem: 'Rota inesperada.' }, 404);
    });
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
    fetchFalso.mockReset();
    vi.restoreAllMocks();
  });

  function abrirPainel(restauranteId = '3') {
    const cliente = criarQueryClient(vi.fn());
    render(
      <NotificacaoProvider>
        <QueryClientProvider client={cliente}>
          <MemoryRouter initialEntries={[`/painel/restaurantes/${restauranteId}/cardapio`]}>
            <Routes>
              <Route
                path="/painel/restaurantes/:restauranteId/cardapio"
                element={<PainelCardapio />}
              />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </NotificacaoProvider>,
    );
  }

  it('cadastra item usando preço em centavos e atualiza a lista', async () => {
    const usuario = userEvent.setup();
    abrirPainel();

    expect(await screen.findByText('Pizza Recife')).toBeTruthy();
    await usuario.click(screen.getByRole('button', { name: 'Novo item' }));
    await usuario.type(screen.getByLabelText('Nome'), 'Suco de laranja');
    await usuario.type(screen.getByLabelText('Descrição'), 'Natural');
    await usuario.type(screen.getByLabelText('Preço (R$)'), '7,50');
    await usuario.selectOptions(screen.getByLabelText('Categoria'), 'BEBIDA');
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar item' }));

    expect(await screen.findByText('Suco de laranja')).toBeTruthy();
    const chamada = fetchFalso.mock.calls.find(
      ([url, init]) =>
        String(url) === `${ORIGEM}/api/restaurantes/3/cardapio` && init?.method === 'POST',
    );
    expect(JSON.parse(String(chamada?.[1]?.body))).toEqual({
      nome: 'Suco de laranja',
      descricao: 'Natural',
      preco: 750,
      disponivel: true,
      urlImagem: null,
      categoria: 'BEBIDA',
    });
  });

  it('edita item preenchendo o preço atual em reais', async () => {
    const usuario = userEvent.setup();
    abrirPainel();

    await screen.findByText('Pizza Recife');
    await usuario.click(screen.getByRole('button', { name: 'Editar Pizza Recife' }));
    expect(screen.getByLabelText('Preço (R$)')).toHaveProperty('value', '25,90');
    const campoNome = screen.getByLabelText('Nome');
    await usuario.clear(campoNome);
    await usuario.type(campoNome, 'Pizza Recife especial');
    await usuario.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(await screen.findByText('Pizza Recife especial')).toBeTruthy();
    const chamada = fetchFalso.mock.calls.find(
      ([url, init]) => String(url) === `${ORIGEM}/api/produtos/7` && init?.method === 'PUT',
    );
    expect(JSON.parse(String(chamada?.[1]?.body))).toMatchObject({
      nome: 'Pizza Recife especial',
      preco: 2590,
    });
  });

  it('abre o formulário sem mensagem de erro', async () => {
    const usuario = userEvent.setup();
    abrirPainel();

    await screen.findByText('Pizza Recife');
    await usuario.click(screen.getByRole('button', { name: 'Novo item' }));
    expect(screen.queryByRole('alert')).toBeNull();

    await usuario.click(screen.getByRole('button', { name: 'Editar Pizza Recife' }));
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('carrega os dados do outro item ao trocar de item com o formulário aberto', async () => {
    produtosServidor = [produtoBase(), { ...produtoBase(), id: 9, nome: 'Calzone', preco: 3150 }];
    const usuario = userEvent.setup();
    abrirPainel();

    await screen.findByText('Calzone');
    await usuario.click(screen.getByRole('button', { name: 'Editar Pizza Recife' }));
    await usuario.click(screen.getByRole('button', { name: 'Editar Calzone' }));
    expect(screen.getByLabelText('Nome')).toHaveProperty('value', 'Calzone');
    expect(screen.getByLabelText('Preço (R$)')).toHaveProperty('value', '31,50');

    await usuario.click(screen.getByRole('button', { name: 'Novo item' }));
    expect(screen.getByLabelText('Nome')).toHaveProperty('value', '');
    expect(screen.getByLabelText('Preço (R$)')).toHaveProperty('value', '');
  });

  it('não mostra no formulário reaberto o erro de um envio anterior', async () => {
    const usuario = userEvent.setup();
    falharProximoCadastro = true;
    abrirPainel();

    await screen.findByText('Pizza Recife');
    await usuario.click(screen.getByRole('button', { name: 'Novo item' }));
    await usuario.type(screen.getByLabelText('Nome'), 'Suco');
    await usuario.type(screen.getByLabelText('Preço (R$)'), '5,00');
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar item' }));
    expect(await screen.findByText('Preço inválido.')).toBeTruthy();

    await usuario.click(screen.getByRole('button', { name: 'Novo item' }));
    expect(screen.queryByText('Preço inválido.')).toBeNull();
  });

  it('fecha o formulário ao excluir o item que está sendo editado', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const usuario = userEvent.setup();
    abrirPainel();

    await screen.findByText('Pizza Recife');
    await usuario.click(screen.getByRole('button', { name: 'Editar Pizza Recife' }));
    await usuario.click(screen.getByRole('button', { name: 'Excluir Pizza Recife' }));

    await waitFor(() => expect(screen.queryByText('Pizza Recife')).toBeNull());
    expect(screen.queryByRole('heading', { name: 'Editar item' })).toBeNull();
  });

  it('alterna disponibilidade e pede confirmação antes de excluir', async () => {
    const usuario = userEvent.setup();
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    abrirPainel();

    await screen.findByText('Pizza Recife');
    await usuario.click(screen.getByRole('button', { name: 'Tornar indisponível: Pizza Recife' }));
    expect(await screen.findByText('Indisponível')).toBeTruthy();
    expect(
      fetchFalso.mock.calls.some(
        ([url, init]) =>
          String(url) === `${ORIGEM}/api/produtos/7/disponibilidade` &&
          init?.method === 'PATCH' &&
          init.body === JSON.stringify({ disponivel: false }),
      ),
    ).toBe(true);

    await usuario.click(screen.getByRole('button', { name: 'Excluir Pizza Recife' }));
    expect(confirmar).toHaveBeenCalled();
    expect(await screen.findByText('Pizza Recife')).toBeTruthy();
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(false);

    confirmar.mockReturnValue(true);
    await usuario.click(screen.getByRole('button', { name: 'Excluir Pizza Recife' }));
    await waitFor(() => expect(screen.queryByText('Pizza Recife')).toBeNull());
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(true);
  });

  it('mostra erro de validação devolvido pela API sem fechar o formulário', async () => {
    const usuario = userEvent.setup();
    falharProximoCadastro = true;
    abrirPainel();

    await screen.findByText('Pizza Recife');
    await usuario.click(screen.getByRole('button', { name: 'Novo item' }));
    await usuario.type(screen.getByLabelText('Nome'), 'Suco');
    await usuario.type(screen.getByLabelText('Preço (R$)'), '5,00');
    await usuario.selectOptions(screen.getByLabelText('Categoria'), 'BEBIDA');
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar item' }));

    expect(await screen.findByText('Preço inválido.')).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Novo item' })).toBeTruthy();
  });

  it('impede cadastro com preço inválido antes de chamar a API', async () => {
    const usuario = userEvent.setup();
    abrirPainel();

    await screen.findByText('Pizza Recife');
    await usuario.click(screen.getByRole('button', { name: 'Novo item' }));
    await usuario.type(screen.getByLabelText('Nome'), 'Suco');
    await usuario.type(screen.getByLabelText('Preço (R$)'), '25,999');
    await usuario.selectOptions(screen.getByLabelText('Categoria'), 'BEBIDA');
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar item' }));

    expect(await screen.findByText('Use só números, com até 2 casas (ex.: 25,90).')).toBeTruthy();
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false);
  });

  it('mostra estado vazio e permite abrir o cadastro quando não há itens', async () => {
    produtosServidor = [];
    const usuario = userEvent.setup();
    abrirPainel();

    expect(await screen.findByText('Este cardápio ainda não tem itens.')).toBeTruthy();
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar primeiro item' }));
    expect(screen.getByRole('heading', { name: 'Novo item' })).toBeTruthy();
  });

  it('mostra erro de carregamento e permite tentar novamente', async () => {
    falharProximaLeitura = true;
    abrirPainel();

    expect(await screen.findByText('Falha temporária ao carregar o cardápio.')).toBeTruthy();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Tentar de novo' }));
    expect(await screen.findByText('Pizza Recife')).toBeTruthy();
  });

  it('não chama a API para um identificador de restaurante inválido', async () => {
    abrirPainel('abc');

    expect(await screen.findByText('Restaurante não encontrado')).toBeTruthy();
    expect(fetchFalso).not.toHaveBeenCalled();
  });
});
