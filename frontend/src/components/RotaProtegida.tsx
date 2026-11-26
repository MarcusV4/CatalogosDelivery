import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { EstadoMensagem } from './EstadoMensagem';
import { useAutenticacao } from '../hooks/useAutenticacao';
import { ehContaDeLoja } from '../services/oauth';

/** Bloqueio de interface para o painel; a API continua responsável pela autorização real. */
export function RotaProtegida() {
  const { sessao } = useAutenticacao();
  const localizacao = useLocation();

  if (!sessao) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ returnTo: `${localizacao.pathname}${localizacao.search}` }}
      />
    );
  }
  if (!ehContaDeLoja(sessao)) {
    return (
      <EstadoMensagem
        tipo="erro"
        titulo="Acesso restrito."
        descricao="O painel do proprietário está disponível somente para contas de loja."
      />
    );
  }
  return <Outlet />;
}
