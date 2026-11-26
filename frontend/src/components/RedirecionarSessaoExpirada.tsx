import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

/** Envia qualquer tela ao login após 401 ou falha de renovação e preserva o destino. */
export function RedirecionarSessaoExpirada() {
  const navegar = useNavigate();
  const localizacao = useLocation();

  useEffect(() => {
    const voltarAoLogin = () => {
      if (localizacao.pathname === '/login') return;
      navegar('/login', {
        replace: true,
        state: {
          returnTo: `${localizacao.pathname}${localizacao.search}${localizacao.hash}`,
        },
      });
    };
    window.addEventListener('auth:unauthorized', voltarAoLogin);
    window.addEventListener('auth:session-expired', voltarAoLogin);
    return () => {
      window.removeEventListener('auth:unauthorized', voltarAoLogin);
      window.removeEventListener('auth:session-expired', voltarAoLogin);
    };
  }, [localizacao.hash, localizacao.pathname, localizacao.search, navegar]);

  return null;
}
