package br.upe.catalogosdelivery.exceptions;

public class RestauranteNaoEncontradoException extends RuntimeException {
    public RestauranteNaoEncontradoException(String message) {
        super(message);
    }

    public RestauranteNaoEncontradoException() {
        super("Error: Restaurante não encontrado");
    }
}
