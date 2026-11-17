package br.upe.catalogosdelivery.cardapio.entities.DTOs;

public record ValidacaoItemResponseDTO(
        Long itemId,
        boolean existe,
        boolean disponivel,
        Long preco,
        Long restauranteId,
        String nome
) {
}
