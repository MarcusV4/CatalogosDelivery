package br.upe.catalogosdelivery.exceptions;

public class DonoNaoEncontrado extends RuntimeException {
    public DonoNaoEncontrado(String message) {
        super(message);
    }

    public DonoNaoEncontrado() {
        super("Error: Restaurante não encontrado");
    }
}
