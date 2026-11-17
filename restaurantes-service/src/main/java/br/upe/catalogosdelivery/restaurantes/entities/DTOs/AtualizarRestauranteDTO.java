package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;

public record AtualizarRestauranteDTO(
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
        Long taxaEntrega,
        Integer tempoEstimadoMin,
        Integer tempoEstimadoMax,
        String urlImagem,
        Long ownerUserId,
        CategoriaRestaurante categoriaRestaurante
) {}
