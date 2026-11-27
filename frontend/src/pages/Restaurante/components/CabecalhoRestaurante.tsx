import { ImagemComFallback } from '../../../components/ImagemComFallback';
import { SeloSituacao } from '../../../components/SeloSituacao';
import type { Restaurante } from '../../../services/restaurantes';
import type { Previsao } from '../../../utils/horarios';
import { textoEntrega, textoTempo } from '../../../utils/restaurante';
import { ROTULOS_CATEGORIA_RESTAURANTE } from '../../../utils/rotulos';
import { avisoDeFechado } from '../../../utils/situacao';

interface CabecalhoRestauranteProps {
  restaurante: Restaurante;
  /** `null` enquanto os horários carregam ou se não for possível prever. */
  previsao: Previsao | null;
  idTitulo: string;
}

export function CabecalhoRestaurante({
  restaurante,
  previsao,
  idTitulo,
}: CabecalhoRestauranteProps) {
  const { restauranteNome } = restaurante;
  const detalhes = [
    ROTULOS_CATEGORIA_RESTAURANTE[restaurante.categoriaRestaurante],
    textoTempo(restaurante),
    textoEntrega(restaurante),
  ].filter(Boolean);
  const aviso = avisoDeFechado(restaurante, previsao);

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
          <SeloSituacao restaurante={restaurante} previsao={previsao} />
        </div>
      </div>
      {aviso ? <p className="aviso">{aviso}</p> : null}
    </header>
  );
}
