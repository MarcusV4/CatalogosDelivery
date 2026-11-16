package br.upe.catalogosdelivery.common.excecoes;

/** Recurso inexistente: responde 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
