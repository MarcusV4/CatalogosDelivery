import { Link } from 'react-router-dom';
import { ImagemComFallback } from '../../../components/ImagemComFallback';
import { SeloSituacao } from '../../../components/SeloSituacao';
import type { ItemBusca } from '../../../services/busca';
import { caminhos } from '../../../utils/caminhos';
import { formatarPreco } from '../../../utils/preco';
import './CartaoPrato.css';

interface CartaoPratoProps {
  item: ItemBusca;
}

/** Situação do restaurante, ou `null` se a Busca não trouxe os dados (Restaurantes fora do ar). */
function situacaoDoRestaurante({ restauranteAbertoAgora, restauranteStatus }: ItemBusca) {
  if (restauranteAbertoAgora === null || restauranteStatus === null) {
    return null;
  }
  return { abertoAgora: restauranteAbertoAgora, statusRestaurante: restauranteStatus };
}

/** Prato encontrado na busca; leva à página do restaurante. Indisponível aparece esmaecido. */
export function CartaoPrato({ item }: CartaoPratoProps) {
  const situacao = situacaoDoRestaurante(item);

  return (
    <Link to={caminhos.restaurante(item.restauranteId)} className="prato__link">
      <article
        className={`prato${item.disponivel ? '' : ' prato--indisponivel'}`}
        aria-disabled={item.disponivel ? undefined : true}
      >
        <ImagemComFallback
          src={item.urlImagem}
          alt=""
          className="prato__imagem"
          fallback={
            <span className="prato__imagem prato__imagem--vazia" aria-hidden="true">
              {item.nome.charAt(0).toUpperCase()}
            </span>
          }
        />
        <div className="prato__info">
          <h3 className="prato__nome">{item.nome}</h3>
          {item.restauranteNome ? (
            <p className="prato__restaurante">
              <span className="mudo pequeno">{item.restauranteNome}</span>
              {situacao ? <SeloSituacao restaurante={situacao} /> : null}
            </p>
          ) : null}
          <p className="prato__preco">{formatarPreco(item.preco)}</p>
          {item.disponivel ? null : <p className="prato__tag">Indisponível no momento</p>}
        </div>
      </article>
    </Link>
  );
}
