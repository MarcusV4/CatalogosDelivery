import { useBuscaRestaurantes } from '../../../hooks/useBuscaRestaurantes';
import {
  paraParametrosRestaurantes,
  temFiltros,
  type FiltrosBusca,
} from '../../../utils/filtrosBusca';
import { CartaoRestaurante } from './CartaoRestaurante';
import { ResultadosBusca, type OpcoesNavegacao } from './ResultadosBusca';

interface ResultadosRestaurantesProps {
  filtros: FiltrosBusca;
  aoMudarPagina: (pagina: number, opcoes?: OpcoesNavegacao) => void;
  aoLimpar: () => void;
}

export function ResultadosRestaurantes({
  filtros,
  aoMudarPagina,
  aoLimpar,
}: ResultadosRestaurantesProps) {
  const consulta = useBuscaRestaurantes(paraParametrosRestaurantes(filtros));

  return (
    <ResultadosBusca
      titulo="Restaurantes"
      singular="restaurante"
      plural="restaurantes"
      consulta={consulta}
      pagina={filtros.pagina}
      aoMudarPagina={aoMudarPagina}
      comFiltros={temFiltros(filtros)}
      aoLimpar={aoLimpar}
      chave={(restaurante) => restaurante.restauranteId}
      renderizar={(restaurante) => <CartaoRestaurante restaurante={restaurante} />}
    />
  );
}
