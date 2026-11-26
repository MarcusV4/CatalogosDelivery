package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Itens do carrinho do Checkout, todos do mesmo restaurante")
public record ValidacaoItensRequestDTO(
        @Schema(description = "Restaurante do pedido", example = "10") Long restauranteId,
        @Schema(description = "Itens a validar (ao menos um)") List<ItemValidacaoRequestDTO> itens
) {
}
