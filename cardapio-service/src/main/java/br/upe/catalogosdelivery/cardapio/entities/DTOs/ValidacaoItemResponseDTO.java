package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

public record ValidacaoItemResponseDTO(
        Long itemId,
        boolean existe,
        boolean disponivel,
        @Schema(description = "Preço em centavos; nulo quando o item não existe", example = "2590", minimum = "0") Long preco,
        Long restauranteId,
        String nome
) {
}
