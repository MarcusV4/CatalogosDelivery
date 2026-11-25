import type { CategoriaProduto, Produto } from '../services/cardapio';
import { ROTULOS_CATEGORIA_PRODUTO } from './rotulos';

/**
 * Ordem das seções do cardápio: entradas → pratos → … → sobremesas → bebidas.
 * O back não define ordem (o enum `CategoriaProduto` é fixo e sem ordem de exibição).
 */
export const ORDEM_CATEGORIAS_PRODUTO = [
  'ENTRADA',
  'SALGADO',
  'LANCHE',
  'PRATO_PRINCIPAL',
  'PIZZA',
  'ORIENTAL',
  'VEGETARIANO',
  'SOBREMESA',
  'BEBIDA',
] as const satisfies readonly CategoriaProduto[];

export interface GrupoCategoria {
  /** Valor recebido do back (pode ser uma categoria nova que o front ainda não conhece). */
  categoria: string;
  rotulo: string;
  produtos: Produto[];
}

const comparadorTexto = new Intl.Collator('pt-BR');

function ehCategoriaConhecida(categoria: string): categoria is CategoriaProduto {
  return Object.hasOwn(ROTULOS_CATEGORIA_PRODUTO, categoria);
}

/** Rótulo com acento; categoria desconhecida vira texto legível (`NOVA_CATEGORIA` → `Nova categoria`). */
export function rotuloCategoriaProduto(categoria: string): string {
  if (ehCategoriaConhecida(categoria)) {
    return ROTULOS_CATEGORIA_PRODUTO[categoria];
  }
  const texto = categoria.replaceAll('_', ' ').toLowerCase();
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** Posição da categoria na ordem fixa; as que não estão na lista ficam depois de todas. */
function posicao(categoria: string): number {
  const indice = (ORDEM_CATEGORIAS_PRODUTO as readonly string[]).indexOf(categoria);
  return indice === -1 ? ORDEM_CATEGORIAS_PRODUTO.length : indice;
}

function compararCategorias(a: string, b: string): number {
  return posicao(a) - posicao(b) || comparadorTexto.compare(a, b);
}

/**
 * Agrupa os produtos por categoria, na ordem de {@link ORDEM_CATEGORIAS_PRODUTO}; categorias
 * fora dela vão para o fim, em ordem alfabética. Dentro de cada grupo, os itens seguem a ordem
 * de cadastro (`id`), já que o back não garante ordem. Itens indisponíveis são mantidos.
 */
export function agruparPorCategoria(produtos: readonly Produto[]): GrupoCategoria[] {
  const porCategoria = new Map<string, Produto[]>();
  for (const produto of produtos) {
    const grupo = porCategoria.get(produto.categoria);
    if (grupo) {
      grupo.push(produto);
    } else {
      porCategoria.set(produto.categoria, [produto]);
    }
  }

  return [...porCategoria.keys()].sort(compararCategorias).map((categoria) => ({
    categoria,
    rotulo: rotuloCategoriaProduto(categoria),
    produtos: (porCategoria.get(categoria) ?? []).toSorted((a, b) => a.id - b.id),
  }));
}
