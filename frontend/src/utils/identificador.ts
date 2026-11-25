/** Inteiro positivo, sem sinal, zeros à esquerda, decimais ou espaços. */
const ID_VALIDO = /^[1-9]\d*$/;

/** Lê o id de um parâmetro de rota (`"12"` → `12`); devolve `null` se não for um id válido. */
export function lerIdDaRota(valor: string | undefined): number | null {
  if (valor === undefined || !ID_VALIDO.test(valor)) {
    return null;
  }
  const id = Number(valor);
  return Number.isSafeInteger(id) ? id : null;
}
