import type { Restaurante } from '../services/restaurantes';
import { formatarPreco } from './preco';

/** `"Entrega grátis"` ou `"Entrega R$ 5,50"` (taxa em centavos). */
export function textoEntrega({ taxaEntrega }: Pick<Restaurante, 'taxaEntrega'>): string {
  return taxaEntrega === 0 ? 'Entrega grátis' : `Entrega ${formatarPreco(taxaEntrega)}`;
}

/** `"Pedido mínimo R$ 20,00"`, ou `null` quando o restaurante não exige valor mínimo. */
export function textoPedidoMinimo({
  pedidoMinimo,
}: Pick<Restaurante, 'pedidoMinimo'>): string | null {
  return pedidoMinimo ? `Pedido mínimo ${formatarPreco(pedidoMinimo)}` : null;
}

/** `"30–45 min"`, ou `null` se o tempo estimado não foi informado. */
export function textoTempo({
  tempoEstimadoMin,
  tempoEstimadoMax,
}: Pick<Restaurante, 'tempoEstimadoMin' | 'tempoEstimadoMax'>): string | null {
  if (tempoEstimadoMin == null || tempoEstimadoMax == null) {
    return null;
  }
  return `${tempoEstimadoMin}–${tempoEstimadoMax} min`;
}

type CamposEndereco = Pick<
  Restaurante,
  'logradouro' | 'numero' | 'complemento' | 'bairro' | 'cidade' | 'estado' | 'cep'
>;

function juntar(partes: readonly (string | null)[], separador: string): string {
  return partes
    .map((parte) => parte?.trim())
    .filter(Boolean)
    .join(separador);
}

/**
 * Endereço em até duas linhas, pulando campos vazios:
 * `["Rua A, 10, Sala 2 · Boa Vista", "Recife/PE · CEP 50000-000"]`.
 */
export function linhasEndereco(endereco: CamposEndereco): string[] {
  const rua = juntar([endereco.logradouro, endereco.numero, endereco.complemento], ', ');
  const cidade = juntar([endereco.cidade, endereco.estado], '/');
  const cep = endereco.cep?.trim() ? `CEP ${endereco.cep.trim()}` : null;
  return [juntar([rua, endereco.bairro], ' · '), juntar([cidade, cep], ' · ')].filter(Boolean);
}
