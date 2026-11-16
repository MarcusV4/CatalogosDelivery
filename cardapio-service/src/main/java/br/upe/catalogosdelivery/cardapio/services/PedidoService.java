package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.clients.RestauranteResumoDTO;
import br.upe.catalogosdelivery.cardapio.entities.*;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ItemPedidoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.PedidoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.PedidoResponseDTO;
import br.upe.catalogosdelivery.cardapio.mappers.PedidoMapper;
import br.upe.catalogosdelivery.cardapio.repositories.ClienteRepo;
import br.upe.catalogosdelivery.cardapio.repositories.PedidoRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    @Autowired
    PedidoRepo pedidoRepo;

    @Autowired
    ClienteRepo clienteRepo;

    @Autowired
    ProdutoRepo produtoRepo;

    private final PedidoMapper pedidoMapper;

    private final RestauranteClient restauranteClient;


    public PedidoResponseDTO realizarPedido(PedidoRequestDTO dto) {

        Cliente cliente = clienteRepo.findById(dto.clienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado!")
                );

        Pedido pedido = new Pedido();

        pedido.setCliente(cliente);
        pedido.setDataPedido(LocalDateTime.now());

        FormaPagamento pagamento = new FormaPagamento();

        pagamento.setTipoFormaPagamento(dto.tipoFormaPagamento());
        pagamento.setBandeira(dto.bandeiraCartao());
        pagamento.setUltimos4Dig(dto.ultimos4Dig());

        pedido.setFormaPagamento(pagamento);


        BigDecimal valorTotal = BigDecimal.ZERO;

        List<ItemPedido> itens = new ArrayList<>();

        for (ItemPedidoRequestDTO itemDTO : dto.itens()) {

            Produto produto = produtoRepo.findById(itemDTO.produtoId())
                    .orElseThrow(() ->
                            new RuntimeException("Produto não encontrado!")
                    );

            if (!produto.isDisponivel()) {
                throw new RuntimeException(
                        "Produto indisponível: " + produto.getNome()
                );
            }

            if (itemDTO.quantidade() == null || itemDTO.quantidade() <= 0) {
                throw new RuntimeException("Quantidade inválida!");
            }

            Long restauranteIdProduto = produto.getCardapio().getRestauranteId();

            if (pedido.getRestauranteId() == null) {

                // Nome e taxa de entrega vêm do serviço de Restaurantes via HTTP
                RestauranteResumoDTO restaurante = restauranteClient.buscarRestaurante(restauranteIdProduto);

                pedido.setRestauranteId(restauranteIdProduto);
                pedido.setNomeRestaurante(restaurante.restauranteNome());
                pedido.setCardapio(produto.getCardapio());


                pedido.setTaxaEntrega(
                        BigDecimal.valueOf(restaurante.taxaEntrega())
                );

            } else if (!pedido.getRestauranteId().equals(restauranteIdProduto)) {

                throw new RuntimeException(
                        "Não é possível realizar um pedido com produtos de restaurantes diferentes!"
                );
            }

            BigDecimal precoUnitario = produto.getPreco();

            BigDecimal subtotal = precoUnitario.multiply(
                    BigDecimal.valueOf(itemDTO.quantidade())
            );

            valorTotal = valorTotal.add(subtotal);

            ItemPedido item = new ItemPedido();

            item.setProduto(produto);
            item.setQuantidade(itemDTO.quantidade());
            item.setPrecoUnitario(precoUnitario);
            item.setPedido(pedido);

            itens.add(item);
        }

        pedido.setItens(itens);
        pedido.setValorTotal(valorTotal.add(pedido.getTaxaEntrega()));

        Pedido pedidoSalvo = pedidoRepo.save(pedido);


        return pedidoMapper.toDTO(pedidoSalvo);
    }

    public PedidoResponseDTO buscarPedidoPorId(Long id) {

        Pedido pedido = pedidoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado!"));

        return pedidoMapper.toDTO(pedido);
    }
}