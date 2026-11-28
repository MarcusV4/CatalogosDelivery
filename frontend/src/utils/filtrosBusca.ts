import type {
  ParametrosBuscaItens,
  ParametrosBuscaRestaurantes,
  SortItens,
  SortRestaurantes,
} from '../services/busca';
import type { CategoriaRestaurante } from '../services/restaurantes';
import { lerInteiroNaoNegativo, lerInteiroPositivo } from './identificador';
import { ROTULOS_CATEGORIA_RESTAURANTE } from './rotulos';

export type AbaBusca = 'restaurantes' | 'pratos';

/**
 * Ordenações de cada aba: a chave vai na URL do front e `sort` vai para a API. A ordenação
 * padrão (nome A–Z) não tem chave e não envia `sort`.
 */
export const ORDENACOES = {
  restaurantes: {
    taxa: { rotulo: 'Menor taxa de entrega', sort: 'taxaEntrega,asc' },
    tempo: { rotulo: 'Menor tempo de entrega', sort: 'tempoEstimado,asc' },
  },
  pratos: {
    'menor-preco': { rotulo: 'Menor preço', sort: 'preco,asc' },
    'maior-preco': { rotulo: 'Maior preço', sort: 'preco,desc' },
  },
} as const satisfies {
  restaurantes: Record<string, { rotulo: string; sort: SortRestaurantes }>;
  pratos: Record<string, { rotulo: string; sort: SortItens }>;
};

export const ROTULO_ORDEM_PADRAO = 'Nome (A–Z)';

export type OrdemRestaurantes = keyof typeof ORDENACOES.restaurantes;
export type OrdemPratos = keyof typeof ORDENACOES.pratos;
export type OrdemBusca = OrdemRestaurantes | OrdemPratos;

/** Estado da tela inicial, guardado na URL. Preços em centavos; `pagina` começa em 1. */
export interface FiltrosBusca {
  aba: AbaBusca;
  nome: string;
  categoria: CategoriaRestaurante | null;
  abertoAgora: boolean;
  entregaGratis: boolean;
  /** Só na aba Pratos. */
  precoMin: number | null;
  precoMax: number | null;
  /** Só na aba Pratos: esconde os itens indisponíveis. */
  disponivel: boolean;
  /** Sempre uma ordenação da aba atual; `null` = padrão (nome A–Z). */
  ordem: OrdemBusca | null;
  pagina: number;
}

/** Nomes dos parâmetros na URL do front. Os de filtro repetem os nomes da API de Busca. */
export const PARAMETROS_URL = {
  aba: 'tipo',
  nome: 'nome',
  categoria: 'categoria',
  abertoAgora: 'abertoAgora',
  entregaGratis: 'entregaGratis',
  precoMin: 'precoMin',
  precoMax: 'precoMax',
  disponivel: 'disponivel',
  ordem: 'ordem',
  pagina: 'pagina',
} as const;

const ABA_PRATOS = 'pratos';
const VERDADEIRO = 'true';
const PRIMEIRA_PAGINA = 1;
/** Múltiplo de 1, 2 e 3 colunas da grade. */
export const TAMANHO_PAGINA = 12;

export const FILTROS_INICIAIS: FiltrosBusca = {
  aba: 'restaurantes',
  nome: '',
  categoria: null,
  abertoAgora: false,
  entregaGratis: false,
  precoMin: null,
  precoMax: null,
  disponivel: false,
  ordem: null,
  pagina: PRIMEIRA_PAGINA,
};

/** O que só vale na aba Pratos; sai ao voltar para Restaurantes. */
const SO_DE_PRATOS = { precoMin: null, precoMax: null, disponivel: false } as const;

function ehCategoriaRestaurante(valor: string | null): valor is CategoriaRestaurante {
  return valor !== null && Object.hasOwn(ROTULOS_CATEGORIA_RESTAURANTE, valor);
}

function ehOrdemRestaurantes(valor: string | null): valor is OrdemRestaurantes {
  return valor !== null && Object.hasOwn(ORDENACOES.restaurantes, valor);
}

function ehOrdemPratos(valor: string | null): valor is OrdemPratos {
  return valor !== null && Object.hasOwn(ORDENACOES.pratos, valor);
}

/** Ordenação válida para a aba; chave inválida ou de outra aba vira `null`. */
export function lerOrdem(aba: AbaBusca, valor: string | null): OrdemBusca | null {
  if (aba === 'pratos') {
    return ehOrdemPratos(valor) ? valor : null;
  }
  return ehOrdemRestaurantes(valor) ? valor : null;
}

/** Opções do seletor "Ordenar por" da aba; `valor` vazio é a ordenação padrão. */
export function opcoesDeOrdem(aba: AbaBusca): { valor: OrdemBusca | ''; rotulo: string }[] {
  const opcoes = Object.entries(ORDENACOES[aba]).map(([valor, { rotulo }]) => ({
    valor: valor as OrdemBusca,
    rotulo,
  }));
  return [{ valor: '', rotulo: ROTULO_ORDEM_PADRAO }, ...opcoes];
}

/** Faixa válida ou nenhuma: mínimo maior que o máximo descarta os dois (a API responderia 400). */
function lerFaixaDePreco(params: URLSearchParams): Pick<FiltrosBusca, 'precoMin' | 'precoMax'> {
  const precoMin = lerInteiroNaoNegativo(params.get(PARAMETROS_URL.precoMin));
  const precoMax = lerInteiroNaoNegativo(params.get(PARAMETROS_URL.precoMax));
  if (precoMin !== null && precoMax !== null && precoMin > precoMax) {
    return { precoMin: null, precoMax: null };
  }
  return { precoMin, precoMax };
}

/** Lê os filtros da URL, ignorando valores inválidos (viram o valor inicial). */
export function lerFiltros(params: URLSearchParams): FiltrosBusca {
  const aba: AbaBusca = params.get(PARAMETROS_URL.aba) === ABA_PRATOS ? 'pratos' : 'restaurantes';
  const categoria = params.get(PARAMETROS_URL.categoria);
  const ligado = (nome: string) => params.get(nome) === VERDADEIRO;
  const soDePratos =
    aba === 'pratos'
      ? { ...lerFaixaDePreco(params), disponivel: ligado(PARAMETROS_URL.disponivel) }
      : SO_DE_PRATOS;

  return {
    aba,
    nome: params.get(PARAMETROS_URL.nome)?.trim() ?? '',
    categoria: ehCategoriaRestaurante(categoria) ? categoria : null,
    abertoAgora: ligado(PARAMETROS_URL.abertoAgora),
    entregaGratis: ligado(PARAMETROS_URL.entregaGratis),
    ...soDePratos,
    ordem: lerOrdem(aba, params.get(PARAMETROS_URL.ordem)),
    pagina: lerInteiroPositivo(params.get(PARAMETROS_URL.pagina)) ?? PRIMEIRA_PAGINA,
  };
}

/** Escreve só o que difere do estado inicial, para a URL ficar curta e estável. */
export function escreverFiltros(filtros: FiltrosBusca): URLSearchParams {
  const params = new URLSearchParams();
  const definir = (nome: string, valor: string | number | null | false) => {
    if (valor !== null && valor !== false && valor !== '') {
      params.set(nome, String(valor));
    }
  };
  definir(PARAMETROS_URL.aba, filtros.aba === 'pratos' && ABA_PRATOS);
  definir(PARAMETROS_URL.nome, filtros.nome.trim());
  definir(PARAMETROS_URL.categoria, filtros.categoria);
  definir(PARAMETROS_URL.abertoAgora, filtros.abertoAgora && VERDADEIRO);
  definir(PARAMETROS_URL.entregaGratis, filtros.entregaGratis && VERDADEIRO);
  if (filtros.aba === 'pratos') {
    definir(PARAMETROS_URL.precoMin, filtros.precoMin);
    definir(PARAMETROS_URL.precoMax, filtros.precoMax);
    definir(PARAMETROS_URL.disponivel, filtros.disponivel && VERDADEIRO);
  }
  definir(PARAMETROS_URL.ordem, lerOrdem(filtros.aba, filtros.ordem));
  definir(PARAMETROS_URL.pagina, filtros.pagina > PRIMEIRA_PAGINA && filtros.pagina);
  return params;
}

type FiltrosEditaveis = Partial<Omit<FiltrosBusca, 'aba' | 'pagina'>>;

/** Aplica uma mudança de filtro ou de ordenação e volta para a primeira página. */
export function comFiltro(filtros: FiltrosBusca, mudanca: FiltrosEditaveis): FiltrosBusca {
  return { ...filtros, ...mudanca, pagina: PRIMEIRA_PAGINA };
}

/**
 * Troca de aba mantendo termo, categoria, "aberto agora" e "entrega grátis". A ordenação sai
 * (as opções de cada aba são outras); faixa de preço e "só disponíveis" só valem em Pratos.
 */
export function trocarAba(filtros: FiltrosBusca, aba: AbaBusca): FiltrosBusca {
  const semPratos = aba === 'restaurantes' ? SO_DE_PRATOS : {};
  return { ...filtros, ...semPratos, ordem: null, aba, pagina: PRIMEIRA_PAGINA };
}

/** Limpa os filtros, o termo e a ordenação, mantendo a aba. */
export function limparFiltros(filtros: FiltrosBusca): FiltrosBusca {
  return { ...FILTROS_INICIAIS, aba: filtros.aba };
}

/** Há algo restringindo os resultados? A ordenação não conta: ela não esconde resultados. */
export function temFiltros(filtros: FiltrosBusca): boolean {
  return (
    filtros.nome.trim() !== '' ||
    filtros.categoria !== null ||
    filtros.abertoAgora ||
    filtros.entregaGratis ||
    filtros.precoMin !== null ||
    filtros.precoMax !== null ||
    filtros.disponivel
  );
}

/** Campos comuns às duas rotas; `undefined` = parâmetro omitido (só `true` é enviado). */
function parametrosComuns(filtros: FiltrosBusca) {
  return {
    nome: filtros.nome.trim() || undefined,
    abertoAgora: filtros.abertoAgora ? (true as const) : undefined,
    entregaGratis: filtros.entregaGratis ? (true as const) : undefined,
  };
}

function paginacao(filtros: FiltrosBusca) {
  return { page: filtros.pagina - PRIMEIRA_PAGINA, size: TAMANHO_PAGINA };
}

export function paraParametrosRestaurantes(filtros: FiltrosBusca): ParametrosBuscaRestaurantes {
  return {
    ...parametrosComuns(filtros),
    categoria: filtros.categoria ?? undefined,
    sort: ehOrdemRestaurantes(filtros.ordem)
      ? ORDENACOES.restaurantes[filtros.ordem].sort
      : undefined,
    ...paginacao(filtros),
  };
}

/** A categoria escolhida é a do restaurante: na rota de itens ela vai em `categoriaRestaurante`. */
export function paraParametrosItens(filtros: FiltrosBusca): ParametrosBuscaItens {
  return {
    ...parametrosComuns(filtros),
    categoriaRestaurante: filtros.categoria ?? undefined,
    precoMin: filtros.precoMin ?? undefined,
    precoMax: filtros.precoMax ?? undefined,
    disponivel: filtros.disponivel ? (true as const) : undefined,
    sort: ehOrdemPratos(filtros.ordem) ? ORDENACOES.pratos[filtros.ordem].sort : undefined,
    ...paginacao(filtros),
  };
}
