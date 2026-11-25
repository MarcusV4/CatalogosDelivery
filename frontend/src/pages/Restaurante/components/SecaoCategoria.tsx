import { idDaSecaoCategoria, type GrupoCategoria } from '../../../utils/cardapio';
import { CartaoProduto } from './CartaoProduto';

interface SecaoCategoriaProps {
  grupo: GrupoCategoria;
}

export function SecaoCategoria({ grupo }: SecaoCategoriaProps) {
  const id = idDaSecaoCategoria(grupo.categoria);

  return (
    <section id={id} className="secao-categoria" aria-labelledby={`${id}-titulo`}>
      <h2 id={`${id}-titulo`}>{grupo.rotulo}</h2>
      <ul className="itens">
        {grupo.produtos.map((produto) => (
          <li key={produto.id}>
            <CartaoProduto produto={produto} />
          </li>
        ))}
      </ul>
    </section>
  );
}
