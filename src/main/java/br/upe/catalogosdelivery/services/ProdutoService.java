package br.upe.catalogosdelivery.services;

import br.upe.catalogosdelivery.entities.Cardapio;
import br.upe.catalogosdelivery.entities.Produto;
import br.upe.catalogosdelivery.repositories.CardapioRepo;
import br.upe.catalogosdelivery.repositories.ProdutoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    ProdutoRepo produtoRepo;

    @Autowired
    CardapioRepo cardapioRepo;

    public Produto cadastrarProduto(Produto produto, Long restauranteId){
        Cardapio cardapio = cardapioRepo
                .findByRestaurante_restauranteId(restauranteId)
                .orElseThrow(() -> new RuntimeException("Cardápio não encontrado!"));

        produto.setCardapio(cardapio);

        return produtoRepo.save(produto);
    }

    public List<Produto> listarProdutosPorRestaurante(Long restauranteId) {

        return produtoRepo
                .findByCardapio_Restaurante_restauranteId(restauranteId);
    }

}
