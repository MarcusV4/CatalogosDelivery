package br.upe.catalogosdelivery.restaurantes.exceptions;

import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;

/** Restaurante inexistente: o GlobalExceptionHandler responde 404 com "Restaurante {id} não encontrado.". */
public class RestauranteNaoEncontradoException extends RecursoNaoEncontradoException {

    private static final String MENSAGEM = "Restaurante %d não encontrado.";

    public RestauranteNaoEncontradoException(Long id) {
        super(MENSAGEM.formatted(id));
    }
}
