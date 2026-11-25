import { ImagemComFallback } from '../../../components/ImagemComFallback';
import type { Produto } from '../../../services/cardapio';
import { formatarPreco } from '../../../utils/preco';

interface CartaoProdutoProps {
  produto: Produto;
}

/** Item do cardápio. Indisponível: esmaecido, `aria-disabled` e o texto "Indisponível no momento". */
export function CartaoProduto({ produto }: CartaoProdutoProps) {
  const { nome, descricao, preco, disponivel, urlImagem } = produto;

  return (
    <article
      className={`item${disponivel ? '' : ' item--indisponivel'}`}
      aria-disabled={disponivel ? undefined : true}
    >
      <div className="item__texto">
        <h3 className="item__nome">{nome}</h3>
        {descricao?.trim() ? <p className="item__desc">{descricao}</p> : null}
        <p className="item__preco">{formatarPreco(preco)}</p>
        {disponivel ? null : <p className="item__tag">Indisponível no momento</p>}
      </div>
      <ImagemComFallback
        src={urlImagem}
        alt={`Foto de ${nome}`}
        className="item__imagem"
        fallback={
          <span className="item__imagem item__imagem--vazia" aria-hidden="true">
            {nome.charAt(0).toUpperCase()}
          </span>
        }
      />
    </article>
  );
}
