package br.upe.catalogosdelivery.busca.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/** Restaurante como retornado pelo serviço de Restaurantes (GET /api/restaurantes). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestauranteDTO(
        Long restauranteId,
        String restauranteNome,
        String restauranteDesc,
        String cidade,
        String estado,
        String urlImagem,
        String statusRestaurante,
        String categoriaRestaurante,
        @Schema(description = "Taxa fixa em centavos", example = "550", minimum = "0") Long taxaEntrega,
        @Schema(description = "Tempo estimado mínimo, em minutos") Integer tempoEstimadoMin,
        @Schema(description = "Tempo estimado máximo, em minutos") Integer tempoEstimadoMax,
        @Schema(description = "Status ABERTO e dentro do horário de funcionamento agora") Boolean abertoAgora
) {
}
