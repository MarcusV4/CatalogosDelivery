package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da validação de um item")
public record ValidacaoItemResponseDTO(
        @Schema(description = "Id do produto informado", example = "101") Long itemId,
        @Schema(description = "O produto existe num cardápio ativo", example = "true") boolean existe,
        @Schema(description = "O produto está disponível para venda", example = "true") boolean disponivel,
        @Schema(description = "Preço em centavos; nulo quando o item não existe", example = "2590", minimum = "0") Long preco,
        @Schema(description = "Restaurante do produto; nulo quando o item não existe", example = "10") Long restauranteId,
        @Schema(description = "Nome do produto; nulo quando o item não existe", example = "Pizza Calabresa") String nome
) {
}
