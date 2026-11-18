package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.clients.RestauranteResumoDTO;
import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ItemValidacaoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItensRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItemResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoPatchDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.mappers.ProdutoMapper;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;

import java.util.List;

@Service
public class ProdutoService {

    /** Status que o serviço de Restaurantes usa para a exclusão lógica. */
    private static final String STATUS_DESATIVADO = "INDISPONIVEL";

    @Autowired
    ProdutoRepo produtoRepo;

    @Autowired
    CardapioRepo cardapioRepo;

    @Autowired
    RestauranteClient restauranteClient;

    @Autowired
    AutorizacaoRestauranteService autorizacaoRestauranteService;

    private final ProdutoMapper produtoMapper = new ProdutoMapper();

    public ProdutoResponseDTO cadastrarProduto(Produto produto, Long restauranteId){
        if (produto == null || produto.getPreco() == null || produto.getPreco() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o preço em centavos, maior ou igual a zero.");
        }

        // Valida no serviço de Restaurantes (404 se não existir, 503 se estiver fora do ar)
        RestauranteResumoDTO restaurante = restauranteClient.buscarRestaurante(restauranteId);
        // Restaurante desativado (exclusão lógica) não recebe novos produtos.
        if (restaurante != null && STATUS_DESATIVADO.equals(restaurante.statusRestaurante())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Restaurante " + restauranteId + " está desativado e não aceita novos produtos.");
        }

        Cardapio cardapio = obterOuCriarCardapio(restauranteId);
        if (cardapio.getExcluidoEm() != null) {
            throw new RecursoNaoEncontradoException("Cardápio do restaurante " + restauranteId + " não encontrado.");
        }
        autorizacaoRestauranteService.validarPermissao(cardapio.getRestauranteId());

        produto.setCardapio(cardapio);

        Produto produtoSalvo = produtoRepo.save(produto);


        return produtoMapper.toDTO(produtoSalvo);
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> listarProdutosPorRestaurante(Long restauranteId) {

        return produtoRepo
                .findByCardapio_RestauranteIdAndCardapio_ExcluidoEmIsNull(restauranteId)
                .stream().map(produtoMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> listarProdutos() {
        return produtoRepo.findByCardapio_ExcluidoEmIsNull().stream().map(produtoMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarProduto(Long id) {
        return produtoMapper.toDTO(obterProdutoAtivo(id));
    }

    @Transactional
    public ProdutoResponseDTO atualizarProduto(Long id, ProdutoRequestDTO request) {
        Produto produto = obterProdutoAtivo(id);
        validarEscrita(produto);
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setDisponivel(request.disponivel());
        produto.setUrlImagem(request.urlImagem());
        produto.setCategoria(request.categoria());
        return produtoMapper.toDTO(produtoRepo.save(produto));
    }

    @Transactional
    public ProdutoResponseDTO atualizarParcialmenteProduto(Long id, ProdutoPatchDTO request) {
        Produto produto = obterProdutoAtivo(id);
        validarEscrita(produto);
        if (request.nome() == null && request.descricao() == null && request.preco() == null
                && request.disponivel() == null && request.urlImagem() == null && request.categoria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um campo para atualizar.");
        }
        if (request.nome() != null) produto.setNome(request.nome());
        if (request.descricao() != null) produto.setDescricao(request.descricao());
        if (request.preco() != null) produto.setPreco(request.preco());
        if (request.disponivel() != null) produto.setDisponivel(request.disponivel());
        if (request.urlImagem() != null) produto.setUrlImagem(request.urlImagem());
        if (request.categoria() != null) produto.setCategoria(request.categoria());
        return produtoMapper.toDTO(produtoRepo.save(produto));
    }

    @Transactional
    public ProdutoResponseDTO alterarDisponibilidade(Long id, boolean disponivel) {
        Produto produto = obterProdutoAtivo(id);
        validarEscrita(produto);
        produto.setDisponivel(disponivel);
        return produtoMapper.toDTO(produtoRepo.save(produto));
    }

    @Transactional
    public void excluirProduto(Long id) {
        Produto produto = obterProdutoAtivo(id);
        validarEscrita(produto);
        produtoRepo.delete(produto);
    }

    Produto obterProdutoAtivo(Long id) {
        return produtoRepo.findById(id)
                .filter(p -> p.getCardapio().getExcluidoEm() == null)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " não encontrado."));
    }

    void validarEscrita(Produto produto) {
        autorizacaoRestauranteService.validarPermissao(produto.getCardapio().getRestauranteId());
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

        return new ValidacaoItemResponseDTO(
                produto.getId(), true, produto.isDisponivel(), produto.getPreco(),
                restauranteDoProduto, produto.getNome());
    }

}
