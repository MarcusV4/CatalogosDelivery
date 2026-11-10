package br.upe.catalogosdelivery.entities.DTOs;

import br.upe.catalogosdelivery.entities.Enums.TipoFormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(
        Long id,
        LocalDateTime dataPedido,
        String nomeCliente,
        Long restauranteId,
        String nomeRestaurante,
        Long cardapioId,
        BigDecimal taxaEntrega,
        BigDecimal valorTotal,
        TipoFormaPagamento formaPagamento,
        List<ItemPedidoResponseDTO> itens
) {
}
