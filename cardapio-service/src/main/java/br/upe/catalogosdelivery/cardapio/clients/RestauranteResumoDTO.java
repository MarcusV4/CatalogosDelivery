package br.upe.catalogosdelivery.cardapio.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Campos do restaurante que o Cardápio usa, lidos de GET /api/restaurantes/{id}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestauranteResumoDTO(
        Long restauranteId,
        String restauranteNome,
        String statusRestaurante,
        Long taxaEntrega
) {
}
