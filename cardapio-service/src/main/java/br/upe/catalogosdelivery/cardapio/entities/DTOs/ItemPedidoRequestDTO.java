package br.upe.catalogosdelivery.cardapio.entities.DTOs;

public record ItemPedidoRequestDTO(
        Long produtoId,
        Integer quantidade
) {
}
