import type { ReactNode } from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from '../Navbar';
import './styles.css';

interface LayoutProps {
  children?: ReactNode;
}

/** Topo fixo e área de conteúdo centralizada. Sem `children`, renderiza a rota filha. */
export function Layout({ children }: LayoutProps) {
  return (
    <div className="layout">
      <a className="layout__pular" href="#conteudo">
        Pular para o conteúdo
      </a>
      <Navbar />
      <main id="conteudo" className="layout__conteudo" tabIndex={-1}>
        {children ?? <Outlet />}
      </main>
    </div>
  );
}
