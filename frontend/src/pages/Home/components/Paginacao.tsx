import { Botao } from '../../../components/Botao';
import { estadoPaginacao } from '../../../utils/paginacao';

interface PaginacaoProps {
  pagina: number;
  totalPaginas: number;
  aoMudar: (pagina: number) => void;
}

/** "Anterior · Página 2 de 5 · Próxima". Some quando há uma página só. */
export function Paginacao({ pagina, totalPaginas, aoMudar }: PaginacaoProps) {
  if (totalPaginas <= 1) {
    return null;
  }
  const { temAnterior, temProxima, texto } = estadoPaginacao(pagina, totalPaginas);

  return (
    <nav className="paginacao" aria-label="Paginação dos resultados">
      <Botao variante="contorno" disabled={!temAnterior} onClick={() => aoMudar(pagina - 1)}>
        Anterior
      </Botao>
      <span className="paginacao__texto">{texto}</span>
      <Botao variante="contorno" disabled={!temProxima} onClick={() => aoMudar(pagina + 1)}>
        Próxima
      </Botao>
    </nav>
  );
}
