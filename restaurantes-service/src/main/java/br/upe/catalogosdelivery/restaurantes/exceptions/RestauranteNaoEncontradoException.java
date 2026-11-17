package br.upe.catalogosdelivery.restaurantes.exceptions;

import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;

/** Restaurante inexistente: o GlobalExceptionHandler responde 404. */
public class RestauranteNaoEncontradoException extends RecursoNaoEncontradoException {
    public RestauranteNaoEncontradoException(String message) {
        super(message);
    }

    public RestauranteNaoEncontradoException() {
        super("Error: Restaurante não encontrado");
    }
}
