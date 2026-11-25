import { describe, expect, it } from 'vitest';
import { linhasEndereco, textoEntrega, textoTempo } from './restaurante';

/** O Intl usa espaço não separável entre "R$" e o número. */
const reais = (texto: string) => texto.replace('R$ ', 'R$\u00a0');

const ENDERECO_COMPLETO = {
  logradouro: 'Rua da Aurora',
  numero: '100',
  complemento: 'Loja 2',
  bairro: 'Boa Vista',
  cidade: 'Recife',
  estado: 'PE',
  cep: '50050-000',
};

describe('textoEntrega', () => {
  it('formata a taxa em centavos ou mostra entrega grátis', () => {
    expect(textoEntrega({ taxaEntrega: 550 })).toBe(reais('Entrega R$ 5,50'));
    expect(textoEntrega({ taxaEntrega: 0 })).toBe('Entrega grátis');
  });
});

describe('textoTempo', () => {
  it('mostra a faixa de tempo ou null se faltar um dos limites', () => {
    expect(textoTempo({ tempoEstimadoMin: 30, tempoEstimadoMax: 45 })).toBe('30–45 min');
    expect(textoTempo({ tempoEstimadoMin: null, tempoEstimadoMax: 45 })).toBeNull();
    expect(textoTempo({ tempoEstimadoMin: 30, tempoEstimadoMax: null })).toBeNull();
  });
});

describe('linhasEndereco', () => {
  it('monta as duas linhas do endereço completo', () => {
    expect(linhasEndereco(ENDERECO_COMPLETO)).toEqual([
      'Rua da Aurora, 100, Loja 2 · Boa Vista',
      'Recife/PE · CEP 50050-000',
    ]);
  });

  it('pula campos nulos ou em branco', () => {
    expect(
      linhasEndereco({ ...ENDERECO_COMPLETO, complemento: null, bairro: '  ', cep: null }),
    ).toEqual(['Rua da Aurora, 100', 'Recife/PE']);
  });

  it('devolve lista vazia sem endereço', () => {
    expect(
      linhasEndereco({
        logradouro: null,
        numero: null,
        complemento: null,
        bairro: null,
        cidade: null,
        estado: null,
        cep: null,
      }),
    ).toEqual([]);
  });
});
