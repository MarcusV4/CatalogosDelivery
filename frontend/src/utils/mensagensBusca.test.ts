import { describe, expect, it } from 'vitest';
import type { ItemBusca } from '../services/busca';
import { ApiError, STATUS_SEM_RESPOSTA } from '../services/http';
import {
  MENSAGEM_BUSCA_INDISPONIVEL,
  MENSAGEM_ERRO_BUSCA,
  faltamDadosDoRestaurante,
  mensagemDeErroDaBusca,
  mensagemDeVazio,
} from './mensagensBusca';

describe('mensagemDeErroDaBusca', () => {
  it.each([STATUS_SEM_RESPOSTA, 500, 502, 503])(
    'status %i: Busca indisponível, sem detalhe técnico',
    (status) => {
      const erro = new ApiError(status, 'O serviço de Restaurantes está indisponível (detalhe).');
      expect(mensagemDeErroDaBusca(erro)).toEqual(MENSAGEM_BUSCA_INDISPONIVEL);
    },
  );

  it('erro desconhecido também vira indisponibilidade', () => {
    expect(mensagemDeErroDaBusca(new TypeError('falhou'))).toEqual(MENSAGEM_BUSCA_INDISPONIVEL);
  });

  it('4xx vira erro genérico da busca', () => {
    expect(mensagemDeErroDaBusca(new ApiError(400, "Valor inválido para 'abertoAgora'."))).toEqual(
      MENSAGEM_ERRO_BUSCA,
    );
  });
});

describe('mensagemDeVazio', () => {
  it('sugere mudar os filtros quando há filtros', () => {
    expect(mensagemDeVazio('prato', true).titulo).toBe(
      'Nenhum prato encontrado com esses filtros.',
    );
  });

  it('não fala de filtros quando não há nenhum', () => {
    expect(mensagemDeVazio('restaurante', false).titulo).toBe(
      'Nenhum restaurante disponível no momento.',
    );
  });
});

describe('faltamDadosDoRestaurante', () => {
  const item = (restauranteNome: string | null): ItemBusca => ({
    id: 1,
    nome: 'Pizza',
    descricao: null,
    preco: 2590,
    disponivel: true,
    categoria: 'PIZZA',
    urlImagem: null,
    restauranteId: 1,
    restauranteNome,
    restauranteStatus: restauranteNome ? 'ABERTO' : null,
    restauranteAbertoAgora: restauranteNome ? true : null,
  });

  it('detecta itens sem o nome do restaurante', () => {
    expect(faltamDadosDoRestaurante([item('Pizzaria'), item(null)])).toBe(true);
    expect(faltamDadosDoRestaurante([item('Pizzaria')])).toBe(false);
    expect(faltamDadosDoRestaurante([])).toBe(false);
  });
});
