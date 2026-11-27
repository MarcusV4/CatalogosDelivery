package br.upe.catalogosdelivery.common.excecoes;

/**
 * Requisição de escrita sem token, ou com token inválido, expirado ou revogado:
 * responde 401 com {@code WWW-Authenticate: Bearer}.
 */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }

    public NaoAutenticadoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
