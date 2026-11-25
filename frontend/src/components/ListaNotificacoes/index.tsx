import type { Notificacao } from '../../context/notificacao';
import './styles.css';

interface ListaNotificacoesProps {
  notificacoes: Notificacao[];
  onFechar: (id: number) => void;
}

export function ListaNotificacoes({ notificacoes, onFechar }: ListaNotificacoesProps) {
  return (
    <div className="notificacoes" role="status" aria-live="polite">
      {notificacoes.map(({ id, mensagem, tipo }) => (
        <div key={id} className={`notificacao notificacao--${tipo}`}>
          <span>{mensagem}</span>
          <button
            type="button"
            className="notificacao__fechar"
            onClick={() => onFechar(id)}
            aria-label="Fechar aviso"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
