package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Campos opcionais para atualização parcial do produto")
public record ProdutoPatchDTO(
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        @Schema(description = "Novo nome") String nome,
        @Schema(description = "Nova descrição") String descricao,
        @PositiveOrZero
        @Schema(description = "Preço em centavos", example = "2590", minimum = "0") Long preco,
        @Schema(description = "Nova disponibilidade") Boolean disponivel,
        @Schema(description = "Nova URL da imagem") String urlImagem,
        @Schema(description = "Nova categoria") CategoriaProduto categoria
) {}
