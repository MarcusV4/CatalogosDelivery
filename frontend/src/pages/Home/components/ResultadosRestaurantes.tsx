import { useBuscaRestaurantes } from '../../../hooks/useBuscaRestaurantes';
import { paraParametrosRestaurantes, type FiltrosBusca } from '../../../utils/filtrosBusca';
import { CartaoRestaurante } from './CartaoRestaurante';
import { ResultadosBusca, type OpcoesNavegacao } from './ResultadosBusca';

interface ResultadosRestaurantesProps {
  filtros: FiltrosBusca;
  aoMudarPagina: (pagina: number, opcoes?: OpcoesNavegacao) => void;
}

export function ResultadosRestaurantes({ filtros, aoMudarPagina }: ResultadosRestaurantesProps) {
  const consulta = useBuscaRestaurantes(paraParametrosRestaurantes(filtros));

  return (
    <ResultadosBusca
      titulo="Restaurantes"
      singular="restaurante"
      plural="restaurantes"
      consulta={consulta}
      pagina={filtros.pagina}
      aoMudarPagina={aoMudarPagina}
      chave={(restaurante) => restaurante.restauranteId}
      renderizar={(restaurante) => <CartaoRestaurante restaurante={restaurante} />}
    />
  );
}
