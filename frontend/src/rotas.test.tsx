// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { isValidElement, type ReactNode } from 'react';
import { RotaProtegida } from './components/RotaProtegida';
import { PainelCardapio } from './pages/PainelCardapio';
import { roteador } from './rotas';

describe('rotas do painel', () => {
  it('coloca a tela de gerenciamento do cardápio dentro da guarda de contas LOJA', () => {
    const rotaPainel = roteador.routes[0]?.children?.find((rota) => rota.path === 'painel');
    const rotaCardapio = rotaPainel?.children?.find(
      (rota) => rota.path === 'restaurantes/:restauranteId/cardapio',
    );

    expect(tipoDoElemento(rotaPainel?.element)).toBe(RotaProtegida);
    expect(tipoDoElemento(rotaCardapio?.element)).toBe(PainelCardapio);
  });
});

function tipoDoElemento(elemento: ReactNode | undefined): unknown {
  return isValidElement(elemento) ? elemento.type : null;
}
