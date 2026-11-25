/** Reais com até 2 casas, vírgula ou ponto como separador decimal, sem sinal nem milhar. */
const PRECO_EM_REAIS = /^(\d+)(?:[.,](\d{1,2}))?$/;
const CENTAVOS_POR_REAL = 100n;
const CASAS_DECIMAIS = 2;
const LIMITE = BigInt(Number.MAX_SAFE_INTEGER);

/**
 * Converte o preço digitado em reais para centavos inteiros, sem ponto flutuante:
 * `"25,90"` e `"25.90"` → `2590`; `"25,9"` → `2590`; `"25"` → `2500`.
 * Devolve `null` para texto vazio, negativo, com letras, com mais de 2 casas ou com
 * separador de milhar (`"1.234,56"`).
 */
export function reaisParaCentavos(texto: string): number | null {
  const partes = PRECO_EM_REAIS.exec(texto.trim());
  if (!partes?.[1]) {
    return null;
  }
  const reais = BigInt(partes[1]);
  const centavos = BigInt((partes[2] ?? '').padEnd(CASAS_DECIMAIS, '0'));
  const total = reais * CENTAVOS_POR_REAL + centavos;
  return total <= LIMITE ? Number(total) : null;
}

/** Centavos para o texto do campo, sem símbolo: `2590` → `"25,90"`; `2500` → `"25"`. */
export function centavosParaReais(centavos: number): string {
  const valor = BigInt(centavos);
  const reais = valor / CENTAVOS_POR_REAL;
  const resto = valor % CENTAVOS_POR_REAL;
  return resto === 0n
    ? reais.toString()
    : `${reais},${resto.toString().padStart(CASAS_DECIMAIS, '0')}`;
}

export const MENSAGEM_PRECO_INVALIDO = 'Use só números, com até 2 casas (ex.: 25,90).';
export const MENSAGEM_FAIXA_INVERTIDA = 'O valor "De" não pode ser maior que o "Até".';

export type ResultadoFaixa =
  | { valida: true; precoMin: number | null; precoMax: number | null }
  | { valida: false; erroMin: string | null; erroMax: string | null; erroFaixa: string | null };

/** Campo em branco não filtra; preenchido, precisa ser um preço válido. */
function lerCampo(texto: string): { centavos: number | null; erro: string | null } {
  if (texto.trim() === '') {
    return { centavos: null, erro: null };
  }
  const centavos = reaisParaCentavos(texto);
  return centavos === null
    ? { centavos: null, erro: MENSAGEM_PRECO_INVALIDO }
    : { centavos, erro: null };
}

/** Valida os campos "De" e "Até" (em reais) e devolve a faixa em centavos, limites incluídos. */
export function lerFaixaDigitada(textoMin: string, textoMax: string): ResultadoFaixa {
  const min = lerCampo(textoMin);
  const max = lerCampo(textoMax);
  if (min.erro || max.erro) {
    return { valida: false, erroMin: min.erro, erroMax: max.erro, erroFaixa: null };
  }
  if (min.centavos !== null && max.centavos !== null && min.centavos > max.centavos) {
    return { valida: false, erroMin: null, erroMax: null, erroFaixa: MENSAGEM_FAIXA_INVERTIDA };
  }
  return { valida: true, precoMin: min.centavos, precoMax: max.centavos };
}
