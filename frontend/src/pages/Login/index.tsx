import type { FormEvent } from 'react';
import { Botao } from '../../components/Botao';
import { useTituloPagina } from '../../hooks/useTituloPagina';
import './styles.css';

/**
 * Tela visual de login, sem chamadas de API.
 * TODO(#31): integrar com o serviço de Identidade (JWT) quando a autenticação existir.
 */
export function Login() {
  useTituloPagina('Entrar');

  const impedirEnvio = (evento: FormEvent<HTMLFormElement>) => evento.preventDefault();

  return (
    <section className="cartao login" aria-labelledby="login-titulo">
      <h1 id="login-titulo">Entrar</h1>
      <p className="aviso" role="note">
        Login ainda indisponível: a integração com o serviço de Identidade será feita na issue #31.
      </p>
      <form className="login__form" onSubmit={impedirEnvio} noValidate>
        <div className="campo">
          <label htmlFor="login-email">E-mail</label>
          <input id="login-email" type="email" name="email" autoComplete="email" />
        </div>
        <div className="campo">
          <label htmlFor="login-senha">Senha</label>
          <input id="login-senha" type="password" name="senha" autoComplete="current-password" />
        </div>
        <Botao type="submit" larguraTotal disabled>
          Entrar
        </Botao>
      </form>
    </section>
  );
}
