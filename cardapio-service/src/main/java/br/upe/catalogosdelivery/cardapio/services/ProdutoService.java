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
import org.springframework.dao.DataIntegrityViolationException;
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

        // Item de restaurante excluído conta como inexistente para o Checkout
        Produto produto = produtoRepo.findById(item.itemId())
                .filter(p -> p.getCardapio().getExcluidoEm() == null)
                .orElse(null);
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
