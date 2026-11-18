package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;

public record AtualizarStatusRestauranteDTO(Long ownerUserId, StatusRestaurante statusRestaurante) {}
