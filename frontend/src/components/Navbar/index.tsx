import { NavLink } from 'react-router-dom';
import { useAutenticacao } from '../../hooks/useAutenticacao';
import { ehContaDeLoja } from '../../services/oauth';
import './styles.css';

export function Navbar() {
  const { sessao, erro, sair } = useAutenticacao();

  return (
    <header className="topo">
      <div className="topo__interno">
        <NavLink to="/" className="topo__marca" aria-label="Zeppel, página inicial">
          zeppel
        </NavLink>
        <nav className="topo__nav" aria-label="Principal">
          <NavLink to="/" end className="topo__link">
            Início
          </NavLink>
          {ehContaDeLoja(sessao) ? (
            <NavLink to="/painel" className="topo__link">
              Painel
            </NavLink>
          ) : null}
          {sessao ? (
            <button type="button" className="topo__link topo__botao" onClick={() => void sair()}>
              Sair
            </button>
          ) : (
            <NavLink to="/login" className="topo__link topo__link--destaque">
              Entrar
            </NavLink>
          )}
        </nav>
      </div>
      {erro ? (
        <p className="topo__erro" role="alert">
          {erro}
        </p>
      ) : null}
    </header>
  );
}
