import { Link } from 'react-router-dom';
import { ImagemComFallback } from '../../../components/ImagemComFallback';
import { SeloSituacao } from '../../../components/SeloSituacao';
import type { RestauranteBusca } from '../../../services/busca';
import { caminhos } from '../../../utils/caminhos';
import { textoEntrega, textoTempo } from '../../../utils/restaurante';
import { ROTULOS_CATEGORIA_RESTAURANTE } from '../../../utils/rotulos';
import './CartaoRestaurante.css';

/** Só os campos que o cartão mostra (servem o restaurante da Busca e o de Restaurantes). */
export type ResumoRestaurante = Pick<
  RestauranteBusca,
  | 'restauranteId'
  | 'restauranteNome'
  | 'urlImagem'
  | 'categoriaRestaurante'
  | 'statusRestaurante'
  | 'abertoAgora'
  | 'taxaEntrega'
  | 'tempoEstimadoMin'
  | 'tempoEstimadoMax'
>;

interface CartaoRestauranteProps {
  restaurante: ResumoRestaurante;
}

export function CartaoRestaurante({ restaurante }: CartaoRestauranteProps) {
  const tempo = textoTempo(restaurante);
  const inicial = restaurante.restauranteNome.charAt(0).toUpperCase();

  return (
    <Link to={caminhos.restaurante(restaurante.restauranteId)} className="restaurante__link">
      <article className={`restaurante${restaurante.abertoAgora ? '' : ' restaurante--fechado'}`}>
        {/* O nome já está no link: a imagem é decorativa (alt vazio). */}
        <ImagemComFallback
          src={restaurante.urlImagem}
          alt=""
          className="restaurante__logo"
          fallback={
            <span className="restaurante__logo" aria-hidden="true">
              {inicial}
            </span>
          }
        />
        <div className="restaurante__info">
          <h3 className="restaurante__nome">{restaurante.restauranteNome}</h3>
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
