import type { ParametrosBuscaItens, ParametrosBuscaRestaurantes } from '../services/busca';
import type { CategoriaRestaurante } from '../services/restaurantes';
import { lerInteiroNaoNegativo, lerInteiroPositivo } from './identificador';
import { ROTULOS_CATEGORIA_RESTAURANTE } from './rotulos';

export type AbaBusca = 'restaurantes' | 'pratos';

/** Estado da tela inicial, guardado na URL. Preços em centavos; `pagina` começa em 1. */
export interface FiltrosBusca {
  aba: AbaBusca;
  nome: string;
  categoria: CategoriaRestaurante | null;
  abertoAgora: boolean;
  /** Só na aba Pratos. */
  precoMin: number | null;
  precoMax: number | null;
  pagina: number;
}

/** Nomes dos parâmetros na URL do front. Os de filtro repetem os nomes da API de Busca. */
export const PARAMETROS_URL = {
  aba: 'tipo',
  nome: 'nome',
  categoria: 'categoria',
  abertoAgora: 'abertoAgora',
  precoMin: 'precoMin',
  precoMax: 'precoMax',
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
  precoMin: null,
  precoMax: null,
  pagina: PRIMEIRA_PAGINA,
};

function ehCategoriaRestaurante(valor: string | null): valor is CategoriaRestaurante {
  return valor !== null && Object.hasOwn(ROTULOS_CATEGORIA_RESTAURANTE, valor);
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
  const faixa = aba === 'pratos' ? lerFaixaDePreco(params) : { precoMin: null, precoMax: null };

  return {
    aba,
    nome: params.get(PARAMETROS_URL.nome)?.trim() ?? '',
    categoria: ehCategoriaRestaurante(categoria) ? categoria : null,
    abertoAgora: params.get(PARAMETROS_URL.abertoAgora) === VERDADEIRO,
    ...faixa,
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
  if (filtros.aba === 'pratos') {
    definir(PARAMETROS_URL.precoMin, filtros.precoMin);
    definir(PARAMETROS_URL.precoMax, filtros.precoMax);
  }
  definir(PARAMETROS_URL.pagina, filtros.pagina > PRIMEIRA_PAGINA && filtros.pagina);
  return params;
}

type FiltrosEditaveis = Partial<Omit<FiltrosBusca, 'aba' | 'pagina'>>;

/** Aplica uma mudança de filtro e volta para a primeira página. */
export function comFiltro(filtros: FiltrosBusca, mudanca: FiltrosEditaveis): FiltrosBusca {
  return { ...filtros, ...mudanca, pagina: PRIMEIRA_PAGINA };
}

/** Troca de aba mantendo termo, categoria e "aberto agora"; a faixa de preço só vale em Pratos. */
export function trocarAba(filtros: FiltrosBusca, aba: AbaBusca): FiltrosBusca {
  const semPreco = aba === 'restaurantes' ? { precoMin: null, precoMax: null } : {};
  return { ...filtros, ...semPreco, aba, pagina: PRIMEIRA_PAGINA };
}

/** Limpa os filtros e o termo, mantendo a aba. */
export function limparFiltros(filtros: FiltrosBusca): FiltrosBusca {
  return { ...FILTROS_INICIAIS, aba: filtros.aba };
}

export function temFiltros(filtros: FiltrosBusca): boolean {
  return (
    filtros.nome.trim() !== '' ||
    filtros.categoria !== null ||
    filtros.abertoAgora ||
    filtros.precoMin !== null ||
    filtros.precoMax !== null
  );
}

/** Campos comuns às duas rotas; `undefined` = parâmetro omitido. */
function parametrosComuns(filtros: FiltrosBusca) {
  return {
    nome: filtros.nome.trim() || undefined,
    abertoAgora: filtros.abertoAgora ? (true as const) : undefined,
  };
}

function paginacao(filtros: FiltrosBusca) {
  return { page: filtros.pagina - PRIMEIRA_PAGINA, size: TAMANHO_PAGINA };
}

export function paraParametrosRestaurantes(filtros: FiltrosBusca): ParametrosBuscaRestaurantes {
  return {
    ...parametrosComuns(filtros),
    categoria: filtros.categoria ?? undefined,
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
    ...paginacao(filtros),
  };
}
