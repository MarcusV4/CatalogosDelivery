import './styles.css';

interface CarregandoProps {
  mensagem?: string;
}

export function Carregando({ mensagem = 'Carregando…' }: CarregandoProps) {
  return (
    <div className="carregando" role="status">
      <span className="carregando__spinner" aria-hidden="true" />
      <span>{mensagem}</span>
    </div>
  );
}
