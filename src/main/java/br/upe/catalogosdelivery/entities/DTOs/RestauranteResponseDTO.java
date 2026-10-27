package br.upe.catalogosdelivery.entities.DTOs;

import br.upe.catalogosdelivery.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.entities.Enums.StatusRestaurante;

public record RestauranteResponseDTO(
        Long restauranteId,
        String restauranteNome,
        String restauranteDesc,
        String cidade,
        String estado,
        String urlImagem,
        Long idDono,
        String nomeDono,
        StatusRestaurante statusRestaurante,
        CategoriaRestaurante categoriaRestaurante
) {}
