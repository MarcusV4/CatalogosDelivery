const CENTAVOS_POR_REAL = 100n;
const CASAS_DECIMAIS = 2;

const formatadorReal = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/**
 * Formata um valor inteiro em centavos como moeda brasileira: `2590` → `"R$ 25,90"`.
 * A conta é feita com inteiros (BigInt), sem passar por ponto flutuante.
 * @throws RangeError se o valor não for um inteiro seguro e não negativo.
 */
export function formatarPreco(centavos: number): string {
  if (!Number.isSafeInteger(centavos) || centavos < 0) {
    throw new RangeError(
      `Preço inválido: ${String(centavos)}. Informe um inteiro de centavos ≥ 0.`,
    );
  }
  const valor = BigInt(centavos);
  const reais = valor / CENTAVOS_POR_REAL;
  const resto = (valor % CENTAVOS_POR_REAL).toString().padStart(CASAS_DECIMAIS, '0');
  // O Intl formata a string decimal exata, sem conversão para float.
  return formatadorReal.format(`${reais}.${resto}` as Intl.StringNumericLiteral);
}
