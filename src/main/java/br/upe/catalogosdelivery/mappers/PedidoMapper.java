package br.upe.catalogosdelivery.mappers;

import br.upe.catalogosdelivery.entities.DTOs.ItemPedidoResponseDTO;
import br.upe.catalogosdelivery.entities.DTOs.PedidoResponseDTO;
import br.upe.catalogosdelivery.entities.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PedidoMapper {

    public PedidoResponseDTO toDTO(Pedido pedido) {

        List<ItemPedidoResponseDTO> itensDTO = pedido.getItens()
                .stream()
                .map(item -> new ItemPedidoResponseDTO(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getPrecoUnitario().multiply(
                                BigDecimal.valueOf(item.getQuantidade())
                        )
                ))
                .toList();

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getDataPedido(),
                pedido.getCliente().getNome(),
                pedido.getRestaurante().getRestauranteId(),
                pedido.getRestaurante().getRestauranteNome(),
                pedido.getCardapio().getIdCardapio(),
                pedido.getTaxaEntrega(),
                pedido.getValorTotal(),
                pedido.getFormaPagamento().getTipoFormaPagamento(),
                itensDTO
        );
    }
}
