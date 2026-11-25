import { createContext } from 'react';

export type TipoNotificacao = 'erro' | 'sucesso' | 'info';

export interface Notificacao {
  id: number;
  mensagem: string;
  tipo: TipoNotificacao;
}

export interface ValorNotificacao {
  notificar: (mensagem: string, tipo?: TipoNotificacao) => void;
}

export const NotificacaoContext = createContext<ValorNotificacao | null>(null);
