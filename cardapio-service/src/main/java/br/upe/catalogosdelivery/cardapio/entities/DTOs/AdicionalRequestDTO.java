package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Dados completos de um adicional")
public record AdicionalRequestDTO(
        @NotBlank @Schema(description = "Nome do extra", example = "Queijo extra") String nome,
        @NotNull @PositiveOrZero
        @Schema(description = "Preço em centavos", example = "350", minimum = "0") Long preco,
        @NotNull @Schema(description = "Disponibilidade do extra") Boolean disponivel
) {}
