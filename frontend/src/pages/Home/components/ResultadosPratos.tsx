import { useBuscaItens } from '../../../hooks/useBuscaItens';
import { paraParametrosItens, temFiltros, type FiltrosBusca } from '../../../utils/filtrosBusca';
import {
  AVISO_DADOS_DO_RESTAURANTE,
  faltamDadosDoRestaurante,
} from '../../../utils/mensagensBusca';
import { CartaoPrato } from './CartaoPrato';
import { ResultadosBusca, type OpcoesNavegacao } from './ResultadosBusca';

interface ResultadosPratosProps {
  filtros: FiltrosBusca;
  aoMudarPagina: (pagina: number, opcoes?: OpcoesNavegacao) => void;
  aoLimpar: () => void;
}

export function ResultadosPratos({ filtros, aoMudarPagina, aoLimpar }: ResultadosPratosProps) {
  const consulta = useBuscaItens(paraParametrosItens(filtros));

  return (
    <ResultadosBusca
      titulo="Pratos"
      singular="prato"
      plural="pratos"
      consulta={consulta}
      pagina={filtros.pagina}
      aoMudarPagina={aoMudarPagina}
      comFiltros={temFiltros(filtros)}
      aoLimpar={aoLimpar}
      aviso={
        consulta.data && faltamDadosDoRestaurante(consulta.data.conteudo)
          ? AVISO_DADOS_DO_RESTAURANTE
          : null
      }
      chave={(item) => item.id}
      renderizar={(item) => <CartaoPrato item={item} />}
    />
  );
}
