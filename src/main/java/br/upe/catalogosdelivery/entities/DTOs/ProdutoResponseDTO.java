package br.upe.catalogosdelivery.entities.DTOs;

import br.upe.catalogosdelivery.entities.Enums.CategoriaProduto;

import java.math.BigDecimal;

public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        boolean disponivel,
        CategoriaProduto categoria
) {
}
