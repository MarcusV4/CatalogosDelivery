package br.upe.catalogosdelivery.entities.DTOs;

public record ItemPedidoRequestDTO(
        Long produtoId,
        Integer quantidade
) {
}
