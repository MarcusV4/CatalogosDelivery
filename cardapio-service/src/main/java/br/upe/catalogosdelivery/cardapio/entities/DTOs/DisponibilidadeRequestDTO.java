package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nova disponibilidade do produto ou adicional")
public record DisponibilidadeRequestDTO(
        @NotNull @Schema(description = "Disponível para pedidos", example = "true") Boolean disponivel) {}
