package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ItemValidacaoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItensRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItemResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.mappers.ProdutoMapper;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    ProdutoRepo produtoRepo;

    @Autowired
    CardapioRepo cardapioRepo;

    @Autowired
    RestauranteClient restauranteClient;

    private final ProdutoMapper produtoMapper = new ProdutoMapper();

    public ProdutoResponseDTO cadastrarProduto(Produto produto, Long restauranteId){
        // Valida no serviço de Restaurantes (404 se não existir, 503 se estiver fora do ar)
        restauranteClient.buscarRestaurante(restauranteId);

        // O cardápio é criado no primeiro produto do restaurante
        Cardapio cardapio = cardapioRepo
                .findByRestauranteId(restauranteId)
                .orElseGet(() -> {
                    Cardapio novo = new Cardapio();
                    novo.setRestauranteId(restauranteId);
                    return cardapioRepo.save(novo);
                });

        produto.setCardapio(cardapio);

        Produto produtoSalvo = produtoRepo.save(produto);


        return produtoMapper.toDTO(produtoSalvo);
    }

    public List<ProdutoResponseDTO> listarProdutosPorRestaurante(Long restauranteId) {

        return produtoRepo
                .findByCardapio_RestauranteId(restauranteId).stream().map(produtoMapper::toDTO).toList();
    }

    public List<ProdutoResponseDTO> listarProdutos() {
        return produtoRepo.findAll().stream().map(produtoMapper::toDTO).toList();
    }

    public List<ValidacaoItemResponseDTO> validarItens(ValidacaoItensRequestDTO request) {
        if (request == null || request.restauranteId() == null
                || request.itens() == null || request.itens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe restauranteId e ao menos um item para validar.");
        }

        return request.itens().stream()
                .map(item -> validarItem(request.restauranteId(), item))
                .toList();
    }

    private ValidacaoItemResponseDTO validarItem(Long restauranteId, ItemValidacaoRequestDTO item) {
        if (item == null || item.itemId() == null
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cada item deve informar itemId e quantidade maior que zero.");
        }

        Produto produto = produtoRepo.findById(item.itemId()).orElse(null);
        if (produto == null) {
            return new ValidacaoItemResponseDTO(item.itemId(), false, false, null, null, null);
        }

        Long restauranteDoProduto = produto.getCardapio().getRestauranteId();
        if (!restauranteId.equals(restauranteDoProduto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Todos os itens devem pertencer ao restaurante informado.");
        }

        long precoCentavos = produto.getPreco().movePointRight(2).longValueExact();
        return new ValidacaoItemResponseDTO(
                produto.getId(), true, produto.isDisponivel(), precoCentavos,
                restauranteDoProduto, produto.getNome());
    }

}
