import type { UseQueryResult } from '@tanstack/react-query';
import { Carregando } from '../../../components/Carregando';
import { EstadoMensagem } from '../../../components/EstadoMensagem';
import type { Produto } from '../../../services/cardapio';
import { mensagemDoErro } from '../../../services/consultas';
import { agruparPorCategoria, idDaSecaoCategoria } from '../../../utils/cardapio';
import { SecaoCategoria } from './SecaoCategoria';

interface CardapioRestauranteProps {
  cardapio: UseQueryResult<Produto[]>;
}

/** Cardápio agrupado por categoria, com atalhos quando há mais de uma. */
export function CardapioRestaurante({ cardapio }: CardapioRestauranteProps) {
  if (cardapio.isPending) {
    return <Carregando mensagem="Carregando cardápio…" />;
  }
  if (cardapio.isError) {
    return (
      <EstadoMensagem
        tipo="erro"
        titulo="Não foi possível carregar o cardápio."
        descricao={mensagemDoErro(cardapio.error)}
        onTentarDeNovo={() => void cardapio.refetch()}
      />
    );
  }

  const grupos = agruparPorCategoria(cardapio.data);
  if (grupos.length === 0) {
    return (
      <EstadoMensagem tipo="vazio" titulo="Este restaurante ainda não cadastrou o cardápio." />
    );
  }

  return (
    <section className="cardapio" aria-label="Cardápio">
      {grupos.length > 1 ? (
        <nav className="chips" aria-label="Categorias do cardápio">
          {grupos.map((grupo) => (
            <a
              key={grupo.categoria}
              className="chip"
              href={`#${idDaSecaoCategoria(grupo.categoria)}`}
            >
              {grupo.rotulo}
            </a>
          ))}
        </nav>
      ) : null}
      {grupos.map((grupo) => (
        <SecaoCategoria key={grupo.categoria} grupo={grupo} />
      ))}
    </section>
  );
}
