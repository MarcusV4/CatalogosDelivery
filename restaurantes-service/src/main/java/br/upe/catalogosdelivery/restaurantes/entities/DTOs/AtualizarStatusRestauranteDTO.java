package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusRestauranteDTO(

        @NotNull(message = "O status do restaurante deve ser informado.")
        StatusRestaurante statusRestaurante

) {}
