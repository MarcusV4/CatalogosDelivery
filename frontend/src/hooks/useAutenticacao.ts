import { useContext } from 'react';
import { ContextoAutenticacao } from '../context/autenticacaoContexto';

export function useAutenticacao() {
  const contexto = useContext(ContextoAutenticacao);
  if (!contexto) throw new Error('useAutenticacao precisa estar dentro de AutenticacaoProvider.');
  return contexto;
}
