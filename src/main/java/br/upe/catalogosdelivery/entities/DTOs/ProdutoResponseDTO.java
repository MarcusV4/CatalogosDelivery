package br.upe.catalogosdelivery.entities.DTOs;

import br.upe.catalogosdelivery.entities.Enums.CategoriaProduto;

public record ProdutoResponseDTO(
        String nome,
        String descricao,
        float preco,
        boolean disponivel,
        CategoriaProduto categoria
) {
}
