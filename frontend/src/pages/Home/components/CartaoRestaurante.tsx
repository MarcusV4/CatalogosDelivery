import { Link } from 'react-router-dom';
import { SeloSituacao } from '../../../components/SeloSituacao';
import type { Restaurante } from '../../../services/restaurantes';
import { caminhos } from '../../../utils/caminhos';
import { textoEntrega, textoTempo } from '../../../utils/restaurante';
import { ROTULOS_CATEGORIA_RESTAURANTE } from '../../../utils/rotulos';
import './CartaoRestaurante.css';

interface CartaoRestauranteProps {
  restaurante: Restaurante;
}

export function CartaoRestaurante({ restaurante }: CartaoRestauranteProps) {
  const tempo = textoTempo(restaurante);

  return (
    <Link to={caminhos.restaurante(restaurante.restauranteId)} className="restaurante__link">
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
    </Link>
  );
}
