import { useBuscaItens } from '../../../hooks/useBuscaItens';
import { paraParametrosItens, type FiltrosBusca } from '../../../utils/filtrosBusca';
import { CartaoPrato } from './CartaoPrato';
import { ResultadosBusca, type OpcoesNavegacao } from './ResultadosBusca';

interface ResultadosPratosProps {
  filtros: FiltrosBusca;
  aoMudarPagina: (pagina: number, opcoes?: OpcoesNavegacao) => void;
}

export function ResultadosPratos({ filtros, aoMudarPagina }: ResultadosPratosProps) {
  const consulta = useBuscaItens(paraParametrosItens(filtros));

  return (
    <ResultadosBusca
      titulo="Pratos"
      singular="prato"
      plural="pratos"
      consulta={consulta}
      pagina={filtros.pagina}
      aoMudarPagina={aoMudarPagina}
      chave={(item) => item.id}
      renderizar={(item) => <CartaoPrato item={item} />}
    />
  );
}
