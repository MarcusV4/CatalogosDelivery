package br.upe.catalogosdelivery.busca.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Restaurante como retornado pelo serviço de Restaurantes (GET /api/restaurantes). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestauranteDTO(
        Long restauranteId,
        String restauranteNome,
        String restauranteDesc,
        String cidade,
        String estado,
        String urlImagem,
        String statusRestaurante,
        String categoriaRestaurante,
        float taxaEntrega
) {
}
