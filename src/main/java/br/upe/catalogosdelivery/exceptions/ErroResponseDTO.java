package br.upe.catalogosdelivery.exceptions;

public record ErroResponseDTO(
        boolean erro,
        String mensagem
) {
}
