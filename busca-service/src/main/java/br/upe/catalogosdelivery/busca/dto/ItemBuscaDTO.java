package br.upe.catalogosdelivery.busca.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Item encontrado na busca, já com os dados do restaurante.
 * Os campos {@code restaurante*} (exceto o id) ficam nulos se o serviço de
 * Restaurantes estiver fora do ar: a busca de itens continua funcionando.
 */
public record ItemBuscaDTO(
        Long id,
        String nome,
        String descricao,
        @Schema(description = "Preço em centavos", example = "2590", minimum = "0") Long preco,
        boolean disponivel,
        String categoria,
        String urlImagem,
        Long restauranteId,
        @Schema(description = "Nulo se o serviço de Restaurantes estiver fora do ar") String restauranteNome,
        @Schema(description = "Nulo se o serviço de Restaurantes estiver fora do ar") String restauranteStatus,
        @Schema(description = "Nulo se o serviço de Restaurantes estiver fora do ar") Boolean restauranteAbertoAgora
) {
}
