package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.mappers.ProdutoMapper;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

}
