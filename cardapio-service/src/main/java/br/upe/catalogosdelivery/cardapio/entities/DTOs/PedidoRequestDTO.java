package br.upe.catalogosdelivery.cardapio.entities.DTOs;

import br.upe.catalogosdelivery.cardapio.entities.Enums.BandeiraCartao;
import br.upe.catalogosdelivery.cardapio.entities.Enums.TipoFormaPagamento;
import br.upe.catalogosdelivery.cardapio.entities.ItemPedido;

import java.util.List;

public record PedidoRequestDTO(
        Long clienteId,
        TipoFormaPagamento tipoFormaPagamento,
        BandeiraCartao bandeiraCartao,
        String ultimos4Dig,
        List<ItemPedidoRequestDTO> itens
) {
}
