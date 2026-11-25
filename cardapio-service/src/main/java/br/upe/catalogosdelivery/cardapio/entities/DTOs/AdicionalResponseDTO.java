package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Adicional de um produto")
public record AdicionalResponseDTO(
        @Schema(description = "Identificador do adicional") Long id,
        @Schema(description = "Nome do extra") String nome,
        @Schema(description = "Preço em centavos", example = "350", minimum = "0") Long preco,
        @Schema(description = "Disponibilidade do extra") boolean disponivel
) {}
