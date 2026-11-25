import { ImagemComFallback } from '../../../components/ImagemComFallback';
import { SeloSituacao } from '../../../components/SeloSituacao';
import type { Restaurante } from '../../../services/restaurantes';
import { textoEntrega, textoTempo } from '../../../utils/restaurante';
import { ROTULOS_CATEGORIA_RESTAURANTE } from '../../../utils/rotulos';

interface CabecalhoRestauranteProps {
  restaurante: Restaurante;
  idTitulo: string;
}

const AVISO_PEDIDOS = 'Você pode ver o cardápio, mas não pedir.';

/** Aviso do protótipo para quando o restaurante não está aberto agora. */
function avisoFechado({ abertoAgora, statusRestaurante }: Restaurante): string | null {
  if (abertoAgora) {
    return null;
  }
  return statusRestaurante === 'INDISPONIVEL'
    ? `Este restaurante está temporariamente indisponível. ${AVISO_PEDIDOS}`
    : `Este restaurante está fechado agora. ${AVISO_PEDIDOS}`;
}

export function CabecalhoRestaurante({ restaurante, idTitulo }: CabecalhoRestauranteProps) {
  const { restauranteNome } = restaurante;
  const detalhes = [
    ROTULOS_CATEGORIA_RESTAURANTE[restaurante.categoriaRestaurante],
    textoTempo(restaurante),
    textoEntrega(restaurante),
  ].filter(Boolean);
  const aviso = avisoFechado(restaurante);

  return (
    <header className="loja">
      <div className="loja__topo">
        <ImagemComFallback
          src={restaurante.urlImagem}
          alt={`Logo de ${restauranteNome}`}
          className="loja__logo"
          fallback={
            <span className="loja__logo loja__logo--inicial" aria-hidden="true">
              {restauranteNome.charAt(0).toUpperCase()}
            </span>
          }
        />
        <div className="loja__dados">
          <h1 id={idTitulo}>{restauranteNome}</h1>
          <p className="loja__detalhes">{detalhes.join(' · ')}</p>
          <SeloSituacao restaurante={restaurante} />
        </div>
      </div>
      {aviso ? <p className="aviso">{aviso}</p> : null}
    </header>
  );
}
