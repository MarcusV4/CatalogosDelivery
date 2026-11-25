import { QueryClientProvider } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { useNotificacao } from '../hooks/useNotificacao';
import { criarQueryClient } from '../services/consultas';

/** TanStack Query ligado aos toasts: erros de API aparecem sozinhos (exceto com `meta.silenciarErro`). */
export function ProvedorConsultas({ children }: { children: ReactNode }) {
  const { notificar } = useNotificacao();
  const [cliente] = useState(() => criarQueryClient((mensagem) => notificar(mensagem, 'erro')));
  return <QueryClientProvider client={cliente}>{children}</QueryClientProvider>;
}
