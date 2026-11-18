package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Campos opcionais para atualização parcial do adicional")
public record AdicionalPatchDTO(
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        @Schema(description = "Novo nome") String nome,
        @PositiveOrZero
        @Schema(description = "Preço em centavos", example = "350", minimum = "0") Long preco,
        @Schema(description = "Nova disponibilidade") Boolean disponivel
) {}
