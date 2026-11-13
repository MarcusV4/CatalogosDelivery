package br.upe.catalogosdelivery.entities.DTOs;

import br.upe.catalogosdelivery.entities.Enums.BandeiraCartao;
import br.upe.catalogosdelivery.entities.Enums.TipoFormaPagamento;
import br.upe.catalogosdelivery.entities.ItemPedido;

import java.util.List;

public record PedidoRequestDTO(
        Long clienteId,
        TipoFormaPagamento tipoFormaPagamento,
        BandeiraCartao bandeiraCartao,
        String ultimos4Dig,
        List<ItemPedidoRequestDTO> itens
) {
}
