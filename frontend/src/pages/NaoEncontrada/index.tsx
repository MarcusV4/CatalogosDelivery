import { Link } from 'react-router-dom';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import '../../components/Botao/styles.css';
import './styles.css';

interface NaoEncontradaProps {
  titulo?: string;
  descricao?: string;
}

/** 404 do front; outras telas reaproveitam com o próprio texto (ex.: restaurante inexistente). */
export function NaoEncontrada({
  titulo = 'Página não encontrada',
  descricao = 'O endereço pode ter mudado ou não existe.',
}: NaoEncontradaProps) {
  useTituloPagina(titulo);

  return (
    <section className="cartao nao-encontrada" aria-labelledby="nao-encontrada-titulo">
      <h1 id="nao-encontrada-titulo">{titulo}</h1>
      <p className="mudo">{descricao}</p>
      <Link to="/" className="botao botao--principal">
        Voltar ao início
      </Link>
    </section>
  );
}
