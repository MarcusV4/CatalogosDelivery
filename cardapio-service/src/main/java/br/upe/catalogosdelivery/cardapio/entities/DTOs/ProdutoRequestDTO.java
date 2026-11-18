package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Dados completos de um produto")
public record ProdutoRequestDTO(
        @NotBlank @Schema(description = "Nome do produto", example = "Pizza") String nome,
        @Schema(description = "Descrição do produto") String descricao,
        @NotNull @PositiveOrZero
        @Schema(description = "Preço em centavos", example = "2590", minimum = "0") Long preco,
        @NotNull @Schema(description = "Disponibilidade do produto") Boolean disponivel,
        @Schema(description = "URL da imagem do produto") String urlImagem,
        @NotNull @Schema(description = "Categoria fixa do produto") CategoriaProduto categoria
) {}
