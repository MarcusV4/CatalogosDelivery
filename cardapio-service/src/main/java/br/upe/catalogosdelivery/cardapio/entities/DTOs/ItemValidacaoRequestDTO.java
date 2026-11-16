package br.upe.catalogosdelivery.cardapio.entities.DTOs;

public record ItemValidacaoRequestDTO(
        Long itemId,
        Integer quantidade
) {
}
