import { Botao } from '../Botao';
import './styles.css';

interface EstadoMensagemProps {
  tipo: 'vazio' | 'erro';
  titulo: string;
  descricao?: string;
  onTentarDeNovo?: () => void;
}

/** Estado de tela vazia ou de erro, com ação opcional para tentar de novo. */
export function EstadoMensagem({ tipo, titulo, descricao, onTentarDeNovo }: EstadoMensagemProps) {
  return (
    <div className={`estado estado--${tipo}`} role={tipo === 'erro' ? 'alert' : undefined}>
      <p className="estado__titulo">{titulo}</p>
      {descricao ? <p className="mudo">{descricao}</p> : null}
      {onTentarDeNovo ? (
        <Botao variante="contorno" onClick={onTentarDeNovo}>
          Tentar de novo
        </Botao>
      ) : null}
    </div>
  );
}
