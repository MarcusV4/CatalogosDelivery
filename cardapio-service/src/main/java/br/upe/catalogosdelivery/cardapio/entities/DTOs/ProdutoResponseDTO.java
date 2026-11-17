package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;

import java.math.BigDecimal;

public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        boolean disponivel,
        CategoriaProduto categoria,
        Long restauranteId
) {
}
