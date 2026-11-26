import { createContext } from 'react';
import type { SessaoOAuth } from '../services/oauth';

export interface AutenticacaoContexto {
  sessao: SessaoOAuth | null;
  erro: string | null;
  entrar: (returnTo?: string) => Promise<void>;
  concluirCallback: (search: string) => Promise<string>;
  sair: () => Promise<void>;
  limparErro: () => void;
}

export const ContextoAutenticacao = createContext<AutenticacaoContexto | null>(null);
