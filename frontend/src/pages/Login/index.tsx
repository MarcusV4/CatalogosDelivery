import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Botao } from '../../components/Botao';
import { Carregando } from '../../components/Carregando';
import { useAutenticacao } from '../../hooks/useAutenticacao';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import { ehContaDeLoja } from '../../services/oauth';
import './styles.css';

/** Inicia o fluxo interativo hospedado pelo serviço de autenticação. */
export function Login() {
  useTituloPagina('Entrar');
  const localizacao = useLocation();
  const { sessao, erro, entrar, sair, limparErro } = useAutenticacao();
  const returnTo =
    typeof localizacao.state === 'object' &&
    localizacao.state !== null &&
    'returnTo' in localizacao.state &&
    typeof localizacao.state.returnTo === 'string'
      ? localizacao.state.returnTo
      : '/';

  if (sessao) {
    return (
      <section className="cartao login" aria-labelledby="login-titulo">
        <h1 id="login-titulo">Sessão ativa</h1>
        <p className="mudo">Você entrou com uma conta do tipo {sessao.tipo}.</p>
        {ehContaDeLoja(sessao) ? <Link to="/painel">Abrir painel da loja</Link> : null}
        {erro ? <p role="alert">{erro}</p> : null}
        <Botao variante="contorno" onClick={() => void sair()}>
          Sair
        </Botao>
      </section>
    );
  }

  return (
    <section className="cartao login" aria-labelledby="login-titulo">
      <h1 id="login-titulo">Entrar</h1>
      <p className="mudo">Você será direcionado à página segura de autenticação.</p>
      {erro ? (
        <p className="login__erro" role="alert">
          {erro}
        </p>
      ) : null}
      <Botao
        larguraTotal
        onClick={() => {
          limparErro();
          void entrar(returnTo);
        }}
      >
        Entrar com sua conta
      </Botao>
    </section>
  );
}

/** Callback do Authorization Code: valida state e finaliza a sessão antes de navegar. */
export function RetornoOAuth() {
  useTituloPagina('Concluindo login');
  const localizacao = useLocation();
  const navegar = useNavigate();
  const { concluirCallback } = useAutenticacao();
  const [erro, setErro] = useState<string | null>(null);
  const iniciado = useRef(false);

  useEffect(() => {
    if (iniciado.current) return;
    iniciado.current = true;
    void concluirCallback(localizacao.search)
      .then((returnTo) => navegar(returnTo, { replace: true }))
      .catch((falha: unknown) => {
        setErro(falha instanceof Error ? falha.message : 'Não foi possível concluir o login.');
      });
  }, [concluirCallback, localizacao.search, navegar]);

  if (erro) {
    return (
      <section className="cartao login" aria-labelledby="callback-titulo">
        <h1 id="callback-titulo">Não foi possível entrar</h1>
        <p className="login__erro" role="alert">
          {erro}
        </p>
        <Botao onClick={() => navegar('/login', { replace: true })}>Voltar ao login</Botao>
      </section>
    );
  }

  return <Carregando mensagem="Concluindo seu login…" />;
}
