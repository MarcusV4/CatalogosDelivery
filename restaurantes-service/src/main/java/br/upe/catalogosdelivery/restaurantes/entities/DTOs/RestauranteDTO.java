package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import io.swagger.v3.oas.annotations.media.Schema;



public record RestauranteDTO(
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
        @Schema(description = "Taxa fixa informada pelo restaurante, em centavos; não é cálculo de frete", example = "550", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED) Long taxaEntrega,
        Integer tempoEstimadoMin,
        Integer tempoEstimadoMax,
        String urlImagem,
        Long ownerUserId,
        StatusRestaurante statusRestaurante,
        CategoriaRestaurante categoriaRestaurante
) {}
