package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

public record ItemValidacaoRequestDTO(
        @Schema(description = "Id do produto", example = "101") Long itemId,
        @Schema(description = "Quantidade, maior que zero", example = "2", minimum = "1") Integer quantidade
) {
}
