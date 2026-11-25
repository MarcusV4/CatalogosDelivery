/** Atraso padrão entre a última tecla e a busca. */
export const ATRASO_DEBOUNCE_MS = 400;

export interface Agendador<T> {
  /** Agenda a ação com `valor`, cancelando o agendamento anterior. */
  agendar(valor: T): void;
  /** Descarta o agendamento pendente, se houver. */
  cancelar(): void;
  /** Executa já o agendamento pendente (ex.: ao apertar Enter); sem pendência, não faz nada. */
  executarAgora(): void;
}

/** Debounce puro: a ação só roda depois de `atrasoMs` sem novos agendamentos, com o último valor. */
export function criarDebounce<T>(acao: (valor: T) => void, atrasoMs: number): Agendador<T> {
  let temporizador: ReturnType<typeof setTimeout> | undefined;
  let pendente: { valor: T } | undefined;

  const cancelar = () => {
    clearTimeout(temporizador);
    temporizador = undefined;
    pendente = undefined;
  };

  const executarAgora = () => {
    const agendado = pendente;
    cancelar();
    if (agendado) {
      acao(agendado.valor);
    }
  };

  return {
    agendar(valor) {
      cancelar();
      pendente = { valor };
      temporizador = setTimeout(executarAgora, atrasoMs);
    },
    cancelar,
    executarAgora,
  };
}
