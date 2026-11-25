import { useQuery } from '@tanstack/react-query';
import { Carregando } from '../../components/Carregando';
import { EstadoMensagem } from '../../components/EstadoMensagem';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { mensagemDoErro } from '../../services/consultas';
import { chavesRestaurantes, listarRestaurantes } from '../../services/restaurantes';
import { CartaoRestaurante } from './components/CartaoRestaurante';
import './styles.css';

/** Verificação da base: lista os restaurantes do serviço de Restaurantes. */
export function Home() {
  useTituloPagina();
  const {
    data: restaurantes,
    error,
    isPending,
    refetch,
  } = useQuery({
    queryKey: chavesRestaurantes.todos,
    queryFn: ({ signal }) => listarRestaurantes(signal),
    // A própria tela mostra o erro com "Tentar de novo"; não precisa do toast.
    meta: { silenciarErro: true },
  });

  return (
    <section className="home" aria-labelledby="home-titulo">
      <div className="home__faixa">
        <h1 id="home-titulo">Fome de quê hoje?</h1>
        <p className="mudo">Restaurantes cadastrados no Catálogo.</p>
      </div>

      {isPending ? (
        <Carregando mensagem="Carregando restaurantes…" />
      ) : error ? (
        <EstadoMensagem
          tipo="erro"
          titulo="Não foi possível carregar os restaurantes."
          descricao={mensagemDoErro(error)}
          onTentarDeNovo={() => void refetch()}
        />
      ) : restaurantes.length === 0 ? (
        <EstadoMensagem
          tipo="vazio"
          titulo="Nenhum restaurante cadastrado ainda."
          descricao="Rode a seed (docker compose run --rm seed) ou cadastre um pelo Swagger."
        />
      ) : (
        <ul className="home__grade" aria-label="Restaurantes">
          {restaurantes.map((restaurante) => (
            <li key={restaurante.restauranteId}>
              <CartaoRestaurante restaurante={restaurante} />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
