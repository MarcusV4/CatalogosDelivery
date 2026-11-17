package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import java.util.List;

public record ValidacaoItensRequestDTO(
        Long restauranteId,
        List<ItemValidacaoRequestDTO> itens
) {
}
