package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.mappers.ProdutoMapper;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

        Cardapio cardapio = obterOuCriarCardapio(restauranteId);

        produto.setCardapio(cardapio);

        Produto produtoSalvo = produtoRepo.save(produto);


        return produtoMapper.toDTO(produtoSalvo);
    }

    public List<ProdutoResponseDTO> listarProdutosPorRestaurante(Long restauranteId) {

        return produtoRepo
                .findByCardapio_RestauranteIdAndCardapio_ExcluidoEmIsNull(restauranteId)
                .stream().map(produtoMapper::toDTO).toList();
    }

    public List<ProdutoResponseDTO> listarProdutos() {
        return produtoRepo.findByCardapio_ExcluidoEmIsNull().stream().map(produtoMapper::toDTO).toList();
    }

    /** O cardápio é criado no primeiro produto do restaurante. */
    private Cardapio obterOuCriarCardapio(Long restauranteId) {
        return cardapioRepo.findByRestauranteId(restauranteId)
                .orElseGet(() -> criarCardapio(restauranteId));
    }

    /**
     * Duas requisições simultâneas podem não encontrar o cardápio e tentar criá-lo.
     * A constraint unique em restaurante_id barra a segunda, que então usa o cardápio
     * criado pela primeira. Funciona porque este método não roda dentro de uma transação
     * maior (o saveAndFlush tem a sua própria), então a falha não invalida a nova consulta.
     */
    private Cardapio criarCardapio(Long restauranteId) {
        Cardapio novo = new Cardapio();
        novo.setRestauranteId(restauranteId);
        try {
            return cardapioRepo.saveAndFlush(novo);
        } catch (DataIntegrityViolationException e) {
            return cardapioRepo.findByRestauranteId(restauranteId).orElseThrow(() -> e);
        }
    }

}
