/** Inteiro positivo, sem sinal, zeros à esquerda, decimais ou espaços. */
const INTEIRO_POSITIVO = /^[1-9]\d*$/;
/** Inteiro ≥ 0, sem sinal, zeros à esquerda, decimais ou espaços. */
const INTEIRO_NAO_NEGATIVO = /^(0|[1-9]\d*)$/;

function lerInteiro(valor: string | null | undefined, formato: RegExp): number | null {
  if (valor == null || !formato.test(valor)) {
    return null;
  }
  const numero = Number(valor);
  return Number.isSafeInteger(numero) ? numero : null;
}

/** `"12"` → `12`; `null` se não for um inteiro positivo seguro. */
export function lerInteiroPositivo(valor: string | null | undefined): number | null {
  return lerInteiro(valor, INTEIRO_POSITIVO);
}

/** `"0"` → `0`; `null` se não for um inteiro não negativo seguro. */
export function lerInteiroNaoNegativo(valor: string | null | undefined): number | null {
  return lerInteiro(valor, INTEIRO_NAO_NEGATIVO);
}

/** Lê o id de um parâmetro de rota (`"12"` → `12`); devolve `null` se não for um id válido. */
export function lerIdDaRota(valor: string | undefined): number | null {
  return lerInteiroPositivo(valor);
}
