package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        @Schema(description = "Preço em centavos", example = "2590", minimum = "0") Long preco,
        boolean disponivel,
        CategoriaProduto categoria,
        Long restauranteId
) {
}
