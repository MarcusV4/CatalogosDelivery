// @vitest-environment jsdom
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { FormularioHorarios } from './FormularioHorarios';

describe('FormularioHorarios', () => {
  afterEach(cleanup);

  it('envia apenas os dias abertos, com horário no formato da API', async () => {
    const usuario = userEvent.setup();
    const aoSalvar = vi.fn();
    render(<FormularioHorarios horarios={[]} salvando={false} aoSalvar={aoSalvar} />);

    await usuario.click(screen.getByLabelText('Segunda'));
    await usuario.clear(screen.getByLabelText('Abre'));
    await usuario.type(screen.getByLabelText('Abre'), '08:30');
    await usuario.clear(screen.getByLabelText('Fecha'));
    await usuario.type(screen.getByLabelText('Fecha'), '17:15');
    await usuario.click(screen.getByRole('button', { name: 'Salvar horários' }));

    expect(aoSalvar).toHaveBeenCalledWith([
      { diaSemana: 'SEGUNDA', horaAbertura: '08:30:00', horaFechamento: '17:15:00' },
    ]);
  });

  it('impede intervalos invertidos ou que cruzem a meia-noite', async () => {
    const usuario = userEvent.setup();
    const aoSalvar = vi.fn();
    render(<FormularioHorarios horarios={[]} salvando={false} aoSalvar={aoSalvar} />);

    await usuario.click(screen.getByLabelText('Terça'));
    await usuario.clear(screen.getByLabelText('Abre'));
    await usuario.type(screen.getByLabelText('Abre'), '22:00');
    await usuario.clear(screen.getByLabelText('Fecha'));
    await usuario.type(screen.getByLabelText('Fecha'), '02:00');
    await usuario.click(screen.getByRole('button', { name: 'Salvar horários' }));

    expect(screen.getByRole('alert').textContent).toContain('no mesmo dia');
    expect(aoSalvar).not.toHaveBeenCalled();
  });

  it('mantém dias não cadastrados como fechados e pode salvar semana vazia', async () => {
    const usuario = userEvent.setup();
    const aoSalvar = vi.fn();
    render(
      <FormularioHorarios
        horarios={[{ diaSemana: 'QUARTA', horaAbertura: '09:00:00', horaFechamento: '18:00:00' }]}
        salvando={false}
        aoSalvar={aoSalvar}
      />,
    );
    expect(screen.getByLabelText('Quarta')).toHaveProperty('checked', true);
    expect(screen.getByLabelText('Segunda')).toHaveProperty('checked', false);
    await usuario.click(screen.getByLabelText('Quarta'));
    await usuario.click(screen.getByRole('button', { name: 'Salvar horários' }));
    expect(aoSalvar).toHaveBeenCalledWith([]);
  });

  it('exibe mensagem 403 recebida da API', () => {
    render(
      <FormularioHorarios
        horarios={[]}
        salvando={false}
        erro="Usuário não é proprietário do restaurante."
        aoSalvar={vi.fn()}
      />,
    );
    expect(screen.getByRole('alert').textContent).toContain('não é proprietário');
  });
});
