import { SeloSituacao } from '../../../components/SeloSituacao';
import type { Restaurante } from '../../../services/restaurantes';
import { formatarPreco } from '../../../utils/preco';
import { ROTULOS_CATEGORIA_RESTAURANTE } from '../../../utils/rotulos';
import './CartaoRestaurante.css';

interface CartaoRestauranteProps {
  restaurante: Restaurante;
}

function textoEntrega({ taxaEntrega }: Restaurante): string {
  return taxaEntrega === 0 ? 'Entrega grátis' : `Entrega ${formatarPreco(taxaEntrega)}`;
}

function textoTempo({ tempoEstimadoMin, tempoEstimadoMax }: Restaurante): string | null {
  if (tempoEstimadoMin == null || tempoEstimadoMax == null) {
    return null;
  }
  return `${tempoEstimadoMin}–${tempoEstimadoMax} min`;
}

export function CartaoRestaurante({ restaurante }: CartaoRestauranteProps) {
  const tempo = textoTempo(restaurante);

  return (
    <article className={`restaurante${restaurante.abertoAgora ? '' : ' restaurante--fechado'}`}>
      <span className="restaurante__logo" aria-hidden="true">
        {restaurante.restauranteNome.charAt(0).toUpperCase()}
      </span>
      <div className="restaurante__info">
        <h2 className="restaurante__nome">{restaurante.restauranteNome}</h2>
        <p className="mudo pequeno">
          {ROTULOS_CATEGORIA_RESTAURANTE[restaurante.categoriaRestaurante]}
          {tempo ? ` · ${tempo}` : null}
        </p>
        <p className="restaurante__rodape">
          <SeloSituacao restaurante={restaurante} />
          <span className="pequeno">{textoEntrega(restaurante)}</span>
        </p>
      </div>
    </article>
  );
}
