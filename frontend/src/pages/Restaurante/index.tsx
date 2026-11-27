import { useParams } from 'react-router-dom';
import { Carregando } from '../../components/Carregando';
import { EstadoMensagem } from '../../components/EstadoMensagem';
import { useCardapio } from '../../hooks/useCardapio';
import { useHorarios } from '../../hooks/useHorarios';
import { useRestaurante } from '../../hooks/useRestaurante';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { mensagemDoErro } from '../../services/consultas';
import { ApiError } from '../../services/http';
import { previsaoDeFuncionamento } from '../../utils/horarios';
import { lerIdDaRota } from '../../utils/identificador';
import { NaoEncontrada } from '../NaoEncontrada';
import { CabecalhoRestaurante } from './components/CabecalhoRestaurante';
import { CardapioRestaurante } from './components/CardapioRestaurante';
import { InfoRestaurante } from './components/InfoRestaurante';
import './styles.css';

const TITULO_NAO_ENCONTRADO = 'Restaurante não encontrado';
const TITULO_PADRAO = 'Restaurante';

function RestauranteNaoEncontrado() {
  return (
    <NaoEncontrada
      titulo={TITULO_NAO_ENCONTRADO}
      descricao="Ele pode ter sido removido ou o endereço está incorreto."
    />
  );
}

/** Rota `/restaurante/:id`. Id inválido vira "não encontrado" sem chamar o back. */
export function PaginaRestaurante() {
  const id = lerIdDaRota(useParams().id);
  if (id === null) {
    return <RestauranteNaoEncontrado />;
  }
  return <ConteudoRestaurante key={id} id={id} />;
}

interface ConteudoRestauranteProps {
  id: number;
}

/** As três consultas correm em paralelo; só o restaurante bloqueia a página. */
function ConteudoRestaurante({ id }: ConteudoRestauranteProps) {
  const restaurante = useRestaurante(id);
  const horarios = useHorarios(id);
  const cardapio = useCardapio(id);

  const naoEncontrado = restaurante.error instanceof ApiError && restaurante.error.ehNaoEncontrado;
  useTituloPagina(
    restaurante.data?.restauranteNome ?? (naoEncontrado ? TITULO_NAO_ENCONTRADO : TITULO_PADRAO),
  );

  if (restaurante.isPending) {
    return <Carregando mensagem="Carregando restaurante…" />;
  }
  if (naoEncontrado) {
    return <RestauranteNaoEncontrado />;
  }
  if (restaurante.isError) {
    const tentarDeNovo = () => {
      void restaurante.refetch();
      void horarios.refetch();
      void cardapio.refetch();
    };
    return (
      <EstadoMensagem
        tipo="erro"
        titulo="Não foi possível carregar o restaurante."
        descricao={mensagemDoErro(restaurante.error)}
        onTentarDeNovo={tentarDeNovo}
      />
    );
  }

  // "Agora" é o instante em que o restaurante foi carregado: o mesmo em que o back calculou o
  // abertoAgora. Assim o texto não diverge dele quando os dados são recarregados mais tarde.
  const previsao = horarios.data
    ? previsaoDeFuncionamento(
        horarios.data,
        restaurante.data.abertoAgora,
        new Date(restaurante.dataUpdatedAt),
      )
    : null;

  return (
    <article className="pagina-restaurante" aria-labelledby="restaurante-nome">
      <CabecalhoRestaurante
        restaurante={restaurante.data}
        previsao={previsao}
        idTitulo="restaurante-nome"
      />
      <InfoRestaurante restaurante={restaurante.data} horarios={horarios} />
      <CardapioRestaurante cardapio={cardapio} />
    </article>
  );
}
