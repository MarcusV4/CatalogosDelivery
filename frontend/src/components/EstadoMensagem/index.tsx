import type { ReactNode } from 'react';
import { Botao } from '../Botao';
import './styles.css';

interface EstadoMensagemProps {
  tipo: 'vazio' | 'erro';
  titulo: string;
  descricao?: string;
  onTentarDeNovo?: () => void;
  /** Ações extras ao lado de "Tentar de novo" (ex.: "Limpar filtros"). */
  acoes?: ReactNode;
}

/** Estado de tela vazia ou de erro, com ações opcionais (tentar de novo e outras). */
export function EstadoMensagem({
  tipo,
  titulo,
  descricao,
  onTentarDeNovo,
  acoes,
}: EstadoMensagemProps) {
  return (
    <div className={`estado estado--${tipo}`} role={tipo === 'erro' ? 'alert' : undefined}>
      <p className="estado__titulo">{titulo}</p>
      {descricao ? <p className="mudo">{descricao}</p> : null}
      {onTentarDeNovo || acoes ? (
        <div className="estado__acoes">
          {onTentarDeNovo ? (
            <Botao variante="contorno" onClick={onTentarDeNovo}>
              Tentar de novo
            </Botao>
          ) : null}
          {acoes}
        </div>
      ) : null}
    </div>
  );
}
