package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import io.swagger.v3.oas.annotations.media.Schema;

public record RestauranteResponseDTO(
        Long restauranteId,
        String restauranteNome,
        String restauranteDesc,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String cep,
        String restauranteTelefone,
        String urlImagem,
        @Schema(description = "Conta dona do restaurante: UUID do sub do token da Identidade", example = "6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01") String ownerUserId,
        StatusRestaurante statusRestaurante,
        CategoriaRestaurante categoriaRestaurante,
        @Schema(description = "Taxa fixa informada pelo restaurante, em centavos", example = "550", minimum = "0") Long taxaEntrega,
        Integer tempoEstimadoMin,
        Integer tempoEstimadoMax,
        boolean abertoAgora
) {}
