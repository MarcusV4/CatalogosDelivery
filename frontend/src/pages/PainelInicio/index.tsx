import { Link } from 'react-router-dom';
import { Carregando } from '../../components/Carregando';
import { EstadoMensagem } from '../../components/EstadoMensagem';
import { useAutenticacao } from '../../hooks/useAutenticacao';
import { useMeusRestaurantes } from '../../hooks/useMeusRestaurantes';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { mensagemDoErro } from '../../services/consultas';
import './styles.css';

const TITULO_PAGINA = 'Painel da loja';

/** Entrada do painel (`/painel`): lista os restaurantes do dono e leva ao cardápio de cada um. */
export function PainelInicio() {
  useTituloPagina(TITULO_PAGINA);
  const { sessao } = useAutenticacao();
  const restaurantes = useMeusRestaurantes(sessao?.usuarioId ?? '');

  if (restaurantes.isPending) {
    return <Carregando mensagem="Carregando seus restaurantes…" />;
  }
  if (restaurantes.isError) {
    return (
      <EstadoMensagem
        tipo="erro"
        titulo="Não foi possível carregar seus restaurantes."
        descricao={mensagemDoErro(restaurantes.error)}
        onTentarDeNovo={() => void restaurantes.refetch()}
      />
    );
  }

  return (
    <section className="painel-inicio" aria-labelledby="painel-inicio-titulo">
      <header className="painel-inicio__cabecalho">
        <h1 id="painel-inicio-titulo">{TITULO_PAGINA}</h1>
        <p className="mudo">Escolha um restaurante para gerenciar o cardápio.</p>
      </header>

      {restaurantes.data.length === 0 ? (
        <EstadoMensagem
          tipo="vazio"
          titulo="Nenhum restaurante ativo está vinculado à sua conta."
          descricao="Restaurantes desativados não aparecem nesta lista."
        />
      ) : (
        <ul className="painel-inicio__lista" aria-label="Seus restaurantes">
          {restaurantes.data.map((restaurante) => (
            <li key={restaurante.restauranteId} className="painel-inicio__item">
              <div>
                <h2>{restaurante.restauranteNome}</h2>
                {restaurante.cidade ? (
                  <p className="mudo">
                    {restaurante.cidade}
                    {restaurante.estado ? ` – ${restaurante.estado}` : ''}
                  </p>
                ) : null}
              </div>
              <Link to={`/painel/restaurantes/${restaurante.restauranteId}/cardapio`}>
                Gerenciar cardápio
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
