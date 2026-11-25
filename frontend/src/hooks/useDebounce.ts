import { useEffect, useMemo, useRef } from 'react';
import { ATRASO_DEBOUNCE_MS, criarDebounce, type Agendador } from '../utils/debounce';

/**
 * Agendador com debounce para uma ação (ex.: gravar o termo na URL depois que a pessoa para
 * de digitar). Usa sempre a versão mais recente de `acao`, é estável entre renderizações e
 * cancela o pendente ao desmontar.
 */
export function useDebounce<T>(
  acao: (valor: T) => void,
  atrasoMs: number = ATRASO_DEBOUNCE_MS,
): Agendador<T> {
  const acaoAtual = useRef(acao);
  const debounce = useRef<Agendador<T> | null>(null);

  useEffect(() => {
    acaoAtual.current = acao;
  });
  // Ao desmontar ou mudar o atraso: descarta o pendente e recria o agendador na próxima chamada.
  useEffect(
    () => () => {
      debounce.current?.cancelar();
      debounce.current = null;
    },
    [atrasoMs],
  );

  return useMemo(() => {
    // Criado na primeira chamada (num evento), nunca durante a renderização.
    const obter = () =>
      (debounce.current ??= criarDebounce<T>((valor) => acaoAtual.current(valor), atrasoMs));
    return {
      agendar: (valor: T) => obter().agendar(valor),
      cancelar: () => debounce.current?.cancelar(),
      executarAgora: () => debounce.current?.executarAgora(),
    };
  }, [atrasoMs]);
}
