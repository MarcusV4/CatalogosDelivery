import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  configuracaoOAuth,
  definirSessaoAtual,
  limparSessaoAtual,
  obterMetadadosOAuth,
  renovarTokens,
  revogarRefreshToken,
  lerSessaoAtual,
  type SessaoOAuth,
} from '../services/oauth';
import { concluirFluxoLogin, iniciarFluxoLogin } from '../services/oauthFlow';
import { ContextoAutenticacao } from './autenticacaoContexto';

export function AutenticacaoProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<SessaoOAuth | null>(() => lerSessaoAtual());
  const [erro, setErro] = useState<string | null>(null);

  const atualizarSessao = useCallback((novaSessao: SessaoOAuth | null) => {
    if (novaSessao) definirSessaoAtual(novaSessao);
    else limparSessaoAtual();
    setSessao(novaSessao);
  }, []);

  const entrar = useCallback(async (returnTo = '/') => {
    setErro(null);
    try {
      await iniciarFluxoLogin(returnTo);
    } catch (falha) {
      setErro(mensagemDoErro(falha));
    }
  }, []);

  const concluirCallback = useCallback(
    async (search: string) => {
      setErro(null);
      const resultado = await concluirFluxoLogin(search);
      atualizarSessao(resultado.sessao);
      return resultado.returnTo;
    },
    [atualizarSessao],
  );

  const sair = useCallback(async () => {
    const sessaoParaRevogar = lerSessaoAtual();
    atualizarSessao(null);
    setErro(null);
    if (!sessaoParaRevogar) return;

    try {
      const config = configuracaoOAuth();
      const metadados = await obterMetadadosOAuth(config);
      await revogarRefreshToken(config, metadados, sessaoParaRevogar.refreshToken);
    } catch (falha) {
      setErro(mensagemDoErro(falha));
    }
  }, [atualizarSessao]);

  const limparErro = useCallback(() => setErro(null), []);

  useEffect(() => {
    const encerrarPor401 = () => {
      atualizarSessao(null);
      setErro('Sua sessão expirou. Entre novamente para continuar.');
    };
    window.addEventListener('auth:unauthorized', encerrarPor401);
    return () => window.removeEventListener('auth:unauthorized', encerrarPor401);
  }, [atualizarSessao]);

  useEffect(() => {
    if (!sessao) return;
    let cancelado = false;
    const margemRenovacaoMs = 60_000;
    const atrasoMs = Math.max(0, sessao.expiraEm - Date.now() - margemRenovacaoMs);
    const temporizador = window.setTimeout(() => {
      void (async () => {
        try {
          const config = configuracaoOAuth();
          const metadados = await obterMetadadosOAuth(config);
          const renovada = await renovarTokens(config, metadados, sessao.refreshToken);
          if (!cancelado) atualizarSessao(renovada);
        } catch {
          if (!cancelado) {
            atualizarSessao(null);
            setErro('Sua sessão expirou. Entre novamente para continuar.');
            window.dispatchEvent(new Event('auth:session-expired'));
          }
        }
      })();
    }, atrasoMs);

    return () => {
      cancelado = true;
      window.clearTimeout(temporizador);
    };
  }, [sessao, atualizarSessao]);

  const valor = useMemo(
    () => ({ sessao, erro, entrar, concluirCallback, sair, limparErro }),
    [sessao, erro, entrar, concluirCallback, sair, limparErro],
  );

  return <ContextoAutenticacao.Provider value={valor}>{children}</ContextoAutenticacao.Provider>;
}

function mensagemDoErro(falha: unknown): string {
  return falha instanceof Error
    ? falha.message
    : 'Não foi possível concluir a operação de autenticação.';
}
