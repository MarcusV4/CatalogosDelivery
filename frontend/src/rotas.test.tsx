// @vitest-environment jsdom
import { describe, expect, it } from 'vitest';
import { isValidElement, type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { RotaProtegida } from './components/RotaProtegida';
import { PainelCardapio } from './pages/PainelCardapio';
import { PainelRestaurantes } from './pages/PainelRestaurantes';
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

  it('coloca a tela de restaurantes na rota protegida do painel da loja', () => {
    const rotaPainel = roteador.routes[0]?.children?.find((rota) => rota.path === 'painel');
    const rotaRestaurantes = rotaPainel?.children?.find((rota) => rota.path === 'restaurantes');

    expect(tipoDoElemento(rotaPainel?.element)).toBe(RotaProtegida);
    expect(tipoDoElemento(rotaRestaurantes?.element)).toBe(PainelRestaurantes);
  });

  it('redireciona /painel para a lista de restaurantes', () => {
    const rotaPainel = roteador.routes[0]?.children?.find((rota) => rota.path === 'painel');
    const rotaInicial = rotaPainel?.children?.find((rota) => rota.index);

    expect(tipoDoElemento(rotaInicial?.element)).toBe(Navigate);
    expect(isValidElement<{ to: string }>(rotaInicial?.element) && rotaInicial.element.props.to).toBe(
      'restaurantes',
    );
  });
});

function tipoDoElemento(elemento: ReactNode | undefined): unknown {
  return isValidElement(elemento) ? elemento.type : null;
}
