package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;

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
        float taxaEntrega
) {}
