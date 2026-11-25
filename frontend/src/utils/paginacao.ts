const PRIMEIRA_PAGINA = 1;

/**
 * Página para onde redirecionar quando a URL pede uma página que não existe, ou `null`
 * se a atual serve. Sem resultados (`totalPaginas = 0`) a página válida é a 1, e a 1
 * nunca é redirecionada: não há laço.
 */
export function corrigirPagina(pagina: number, totalPaginas: number): number | null {
  const ultima = Math.max(totalPaginas, PRIMEIRA_PAGINA);
  return pagina > ultima ? ultima : null;
}

export interface EstadoPaginacao {
  temAnterior: boolean;
  temProxima: boolean;
  /** `"Página 2 de 5"`. */
  texto: string;
}

/** Botões e texto da paginação (`pagina` começa em 1). */
export function estadoPaginacao(pagina: number, totalPaginas: number): EstadoPaginacao {
  const total = Math.max(totalPaginas, PRIMEIRA_PAGINA);
  return {
    temAnterior: pagina > PRIMEIRA_PAGINA,
    temProxima: pagina < total,
    texto: `Página ${pagina} de ${total}`,
  };
}

/** `"1 restaurante encontrado"`, `"3 pratos encontrados"`, `"Nenhum prato encontrado"`. */
export function textoTotal(total: number, singular: string, plural: string): string {
  if (total === 0) {
    return `Nenhum ${singular} encontrado`;
  }
  return total === 1 ? `1 ${singular} encontrado` : `${total} ${plural} encontrados`;
}
