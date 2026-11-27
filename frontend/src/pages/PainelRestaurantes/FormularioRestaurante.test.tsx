// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { FormularioRestaurante } from './FormularioRestaurante';

describe('FormularioRestaurante', () => {
  afterEach(cleanup);

  it('valida campos obrigatórios antes de chamar a API', async () => {
    const usuario = userEvent.setup();
    const aoSalvar = vi.fn();
    render(<FormularioRestaurante salvando={false} aoSalvar={aoSalvar} aoCancelar={vi.fn()} />);

    await usuario.click(screen.getByRole('button', { name: 'Cadastrar restaurante' }));

    expect(screen.getByRole('alert').textContent).toContain('Preencha todos os campos');
    expect(aoSalvar).not.toHaveBeenCalled();
  });

  it('envia taxa em centavos e normaliza estado e campos opcionais', async () => {
    const usuario = userEvent.setup();
    const aoSalvar = vi.fn();
    render(<FormularioRestaurante salvando={false} aoSalvar={aoSalvar} aoCancelar={vi.fn()} />);
    const valores: Record<string, string> = {
      'restaurante-nome': '  Cantina da Praça  ',
      'restaurante-descricao': 'Massas frescas',
      'restaurante-telefone': '81999990000',
      'restaurante-logradouro': 'Rua das Flores',
      'restaurante-numero': '12',
      'restaurante-bairro': 'Centro',
      'restaurante-cidade': 'Recife',
      'restaurante-estado': 'pe',
      'restaurante-cep': '50000-000',
      'restaurante-taxa': '5,50',
      'restaurante-tempo-min': '25',
      'restaurante-tempo-max': '45',
    };
    for (const [id, valor] of Object.entries(valores)) {
      await usuario.type(document.getElementById(id) as HTMLElement, valor);
    }
    await usuario.selectOptions(screen.getByLabelText('Categoria'), 'PIZZARIA');
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar restaurante' }));

    expect(aoSalvar).toHaveBeenCalledWith(
      expect.objectContaining({
        restauranteNome: 'Cantina da Praça',
        estado: 'PE',
        taxaEntrega: 550,
        tempoEstimadoMin: 25,
        tempoEstimadoMax: 45,
        complemento: null,
        urlImagem: null,
        pedidoMinimo: null,
        categoriaRestaurante: 'PIZZARIA',
      }),
    );
  });

  it('envia o pedido mínimo em centavos e recusa um valor inválido', async () => {
    const usuario = userEvent.setup();
    const aoSalvar = vi.fn();
    render(<FormularioRestaurante salvando={false} aoSalvar={aoSalvar} aoCancelar={vi.fn()} />);
    const valores: Record<string, string> = {
      'restaurante-nome': 'Cantina',
      'restaurante-descricao': 'Massas',
      'restaurante-telefone': '81999990000',
      'restaurante-logradouro': 'Rua A',
      'restaurante-numero': '1',
      'restaurante-bairro': 'Centro',
      'restaurante-cidade': 'Recife',
      'restaurante-estado': 'PE',
      'restaurante-cep': '50000-000',
      'restaurante-taxa': '0',
      'restaurante-pedido-minimo': '-5',
      'restaurante-tempo-min': '20',
      'restaurante-tempo-max': '30',
    };
    for (const [id, valor] of Object.entries(valores)) {
      await usuario.type(document.getElementById(id) as HTMLElement, valor);
    }
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar restaurante' }));

    expect(screen.getByRole('alert').textContent).toContain('pedido mínimo válido');
    expect(aoSalvar).not.toHaveBeenCalled();

    const campo = screen.getByLabelText('Pedido mínimo (R$, opcional)');
    await usuario.clear(campo);
    await usuario.type(campo, '20,50');
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar restaurante' }));

    expect(aoSalvar).toHaveBeenCalledWith(expect.objectContaining({ pedidoMinimo: 2050 }));
  });

  it('não permite tempo máximo menor que o mínimo', async () => {
    const usuario = userEvent.setup();
    const aoSalvar = vi.fn();
    render(<FormularioRestaurante salvando={false} aoSalvar={aoSalvar} aoCancelar={vi.fn()} />);
    const valores: Record<string, string> = {
      'restaurante-nome': 'Cantina',
      'restaurante-descricao': 'Massas',
      'restaurante-telefone': '81999990000',
      'restaurante-logradouro': 'Rua A',
      'restaurante-numero': '1',
      'restaurante-bairro': 'Centro',
      'restaurante-cidade': 'Recife',
      'restaurante-estado': 'PE',
      'restaurante-cep': '50000-000',
      'restaurante-taxa': '0',
      'restaurante-tempo-min': '50',
      'restaurante-tempo-max': '30',
    };
    for (const [id, valor] of Object.entries(valores)) {
      await usuario.type(document.getElementById(id) as HTMLElement, valor);
    }
    await usuario.click(screen.getByRole('button', { name: 'Cadastrar restaurante' }));

    expect(screen.getByRole('alert').textContent).toContain('não pode ser menor');
    expect(aoSalvar).not.toHaveBeenCalled();
  });

  it('mostra erros do servidor e impede alterações durante o salvamento', () => {
    render(
      <FormularioRestaurante
        salvando
        erro="Usuário não é proprietário do restaurante."
        aoSalvar={vi.fn()}
        aoCancelar={vi.fn()}
      />,
    );
    expect(screen.getByRole('alert').textContent).toContain('não é proprietário');
    expect(screen.getByRole('button', { name: 'Salvando…' })).toHaveProperty('disabled', true);
  });
});
