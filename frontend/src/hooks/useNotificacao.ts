import { useContext } from 'react';
import { NotificacaoContext, type ValorNotificacao } from '../context/notificacao';

/** Mostra um toast: `notificar('Salvo!', 'sucesso')`. Exige o `NotificacaoProvider`. */
export function useNotificacao(): ValorNotificacao {
  const contexto = useContext(NotificacaoContext);
  if (!contexto) {
    throw new Error('useNotificacao precisa estar dentro de <NotificacaoProvider>.');
  }
  return contexto;
}
