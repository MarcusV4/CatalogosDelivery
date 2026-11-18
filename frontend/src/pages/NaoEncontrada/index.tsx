import { Link } from 'react-router-dom';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import '../../components/Botao/styles.css';
import './styles.css';

export function NaoEncontrada() {
  useTituloPagina('Página não encontrada');

  return (
    <section className="cartao nao-encontrada" aria-labelledby="nao-encontrada-titulo">
      <h1 id="nao-encontrada-titulo">Página não encontrada</h1>
      <p className="mudo">O endereço pode ter mudado ou não existe.</p>
      <Link to="/" className="botao botao--principal">
        Voltar ao início
      </Link>
    </section>
  );
}
