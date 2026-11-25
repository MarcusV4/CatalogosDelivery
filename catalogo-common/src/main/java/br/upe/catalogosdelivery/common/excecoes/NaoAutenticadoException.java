package br.upe.catalogosdelivery.common.excecoes;

/** Requisição sem token, com token malformado ou recusado pela Identidade: responde 401. */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
