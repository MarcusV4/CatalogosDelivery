import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { ListaNotificacoes } from '../components/ListaNotificacoes';
import { NotificacaoContext, type Notificacao, type TipoNotificacao } from './notificacao';

const DURACAO_MS = 5_000;
const MAXIMO_VISIVEIS = 3;

/** Toasts da aplicação. Mensagens repetidas enquanto visíveis não se acumulam. */
export function NotificacaoProvider({ children }: { children: ReactNode }) {
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const proximoId = useRef(0);
  const temporizadores = useRef(new Map<number, number>());

  const fechar = useCallback((id: number) => {
    window.clearTimeout(temporizadores.current.get(id));
    temporizadores.current.delete(id);
    setNotificacoes((atuais) => atuais.filter((n) => n.id !== id));
  }, []);

  const notificar = useCallback(
    (mensagem: string, tipo: TipoNotificacao = 'info') => {
      const id = ++proximoId.current;
      setNotificacoes((atuais) => {
        if (atuais.some((n) => n.mensagem === mensagem && n.tipo === tipo)) {
          return atuais;
        }
        return [...atuais, { id, mensagem, tipo }].slice(-MAXIMO_VISIVEIS);
      });
      temporizadores.current.set(
        id,
        window.setTimeout(() => fechar(id), DURACAO_MS),
      );
    },
    [fechar],
  );

  useEffect(() => {
    const ativos = temporizadores.current;
    return () => ativos.forEach((temporizador) => window.clearTimeout(temporizador));
  }, []);

  const valor = useMemo(() => ({ notificar }), [notificar]);

  return (
    <NotificacaoContext.Provider value={valor}>
      {children}
      <ListaNotificacoes notificacoes={notificacoes} onFechar={fechar} />
    </NotificacaoContext.Provider>
  );
}
