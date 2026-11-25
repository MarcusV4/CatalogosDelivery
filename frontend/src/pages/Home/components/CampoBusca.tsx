import { useState } from 'react';
import { useDebounce } from '../../../hooks/useDebounce';

interface CampoBuscaProps {
  /** Termo aplicado (da URL). */
  nome: string;
  placeholder: string;
  aoBuscar: (nome: string) => void;
}

/** Campo de busca por nome: aplica depois de uma pausa na digitação, ou na hora com Enter. */
export function CampoBusca({ nome, placeholder, aoBuscar }: CampoBuscaProps) {
  const [texto, setTexto] = useState(nome);
  const [nomeAplicado, setNomeAplicado] = useState(nome);
  const agendador = useDebounce((termo: string) => aoBuscar(termo));

  // Termo mudou por fora (voltar do navegador, "Limpar filtros"): mostra o novo, sem apagar
  // o espaço que a pessoa acabou de digitar no fim do texto.
  if (nome !== nomeAplicado) {
    setNomeAplicado(nome);
    if (nome !== texto.trim()) {
      setTexto(nome);
    }
  }

  return (
    <form
      role="search"
      className="busca"
      onSubmit={(evento) => {
        evento.preventDefault();
        agendador.executarAgora();
      }}
    >
      <label htmlFor="busca-termo" className="sr">
        Buscar por nome
      </label>
      <input
        id="busca-termo"
        type="search"
        autoComplete="off"
        placeholder={placeholder}
        value={texto}
        onChange={(evento) => {
          setTexto(evento.target.value);
          agendador.agendar(evento.target.value.trim());
        }}
      />
    </form>
  );
}
