package br.upe.catalogosdelivery.busca.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Produto como retornado pelo serviço de Cardápio (GET /api/produtos). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProdutoDTO(
        Long id,
        String nome,
        String descricao,
        Long preco,
        boolean disponivel,
        String categoria,
        Long restauranteId
) {
}
