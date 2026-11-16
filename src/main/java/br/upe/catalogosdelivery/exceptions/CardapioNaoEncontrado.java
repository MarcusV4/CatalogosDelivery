package br.upe.catalogosdelivery.exceptions;

public class CardapioNaoEncontrado extends RuntimeException {
    public CardapioNaoEncontrado(String message) {
        super(message);
    }

    public CardapioNaoEncontrado() {
        super("Error: Restaurante não encontrado");
    }
}
