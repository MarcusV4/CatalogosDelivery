import { NavLink } from 'react-router-dom';
import './styles.css';

export function Navbar() {
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
          <NavLink to="/login" className="topo__link topo__link--destaque">
            Entrar
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
