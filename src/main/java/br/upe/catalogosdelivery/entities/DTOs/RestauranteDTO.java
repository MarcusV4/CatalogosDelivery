package br.upe.catalogosdelivery.entities.DTOs;

import br.upe.catalogosdelivery.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.entities.Enums.StatusRestaurante;



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
        float taxaEntrega,
        Integer tempoEstimadoMin,
        Integer tempoEstimadoMax,
        String urlImagem,
        Long idDono,
        StatusRestaurante statusRestaurante,
        CategoriaRestaurante categoriaRestaurante
) {}
