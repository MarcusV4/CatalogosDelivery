package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import io.swagger.v3.oas.annotations.media.Schema;

public record RestauranteResponseDTO(
        Long restauranteId,
        String restauranteNome,
        String restauranteDesc,
        String cidade,
        String estado,
        String urlImagem,
        Long ownerUserId,
        StatusRestaurante statusRestaurante,
        CategoriaRestaurante categoriaRestaurante,
        @Schema(description = "Taxa fixa informada pelo restaurante, em centavos", example = "550", minimum = "0") Long taxaEntrega
) {}
