package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Produto e seus adicionais de um cardápio ativo")
public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        @Schema(description = "Preço em centavos", example = "2590", minimum = "0") Long preco,
        boolean disponivel,
        CategoriaProduto categoria,
        @Schema(description = "Identificador do restaurante no serviço de Restaurantes") Long restauranteId,
        @Schema(description = "URL da imagem do produto") String urlImagem,
        @Schema(description = "Extras cadastrados para o produto") List<AdicionalResponseDTO> adicionais
) {
}
