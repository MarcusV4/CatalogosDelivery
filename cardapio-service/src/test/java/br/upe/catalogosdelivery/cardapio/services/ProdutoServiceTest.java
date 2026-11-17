package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ItemValidacaoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItemResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItensRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProdutoServiceTest {

    private ProdutoRepo produtoRepo;
    private ProdutoService produtoService;

    @BeforeEach
    void setUp() {
        produtoRepo = mock(ProdutoRepo.class);
        produtoService = new ProdutoService();
        produtoService.produtoRepo = produtoRepo;
    }

    @Test
    void retornaDadosDoProdutoDisponivelComPrecoEmCentavos() {
        when(produtoRepo.findById(101L)).thenReturn(Optional.of(produto(101L, 10L, true)));

        List<ValidacaoItemResponseDTO> resultado = produtoService.validarItens(request(10L, 101L, 2));

        assertEquals(List.of(new ValidacaoItemResponseDTO(
                101L, true, true, 4590L, 10L, "Pizza Calabresa")), resultado);
    }

    @Test
    void retornaItemInexistenteQuandoProdutoNaoEstaNoRepositorio() {
        when(produtoRepo.findById(999L)).thenReturn(Optional.empty());

        List<ValidacaoItemResponseDTO> resultado = produtoService.validarItens(request(10L, 999L, 1));

        assertEquals(List.of(new ValidacaoItemResponseDTO(
                999L, false, false, null, null, null)), resultado);
    }

    @Test
    void retornaItemInexistenteQuandoORestauranteFoiExcluido() {
        Produto produto = produto(101L, 10L, true);
        produto.getCardapio().setExcluidoEm(Instant.parse("2026-11-16T12:00:00Z"));
        when(produtoRepo.findById(101L)).thenReturn(Optional.of(produto));

        List<ValidacaoItemResponseDTO> resultado = produtoService.validarItens(request(10L, 101L, 1));

        assertEquals(List.of(new ValidacaoItemResponseDTO(
                101L, false, false, null, null, null)), resultado);
    }

    @Test
    void retornaProdutoIndisponivelSemAlterarSuaDisponibilidade() {
        when(produtoRepo.findById(101L)).thenReturn(Optional.of(produto(101L, 10L, false)));

        List<ValidacaoItemResponseDTO> resultado = produtoService.validarItens(request(10L, 101L, 1));

        assertEquals(List.of(new ValidacaoItemResponseDTO(
                101L, true, false, 4590L, 10L, "Pizza Calabresa")), resultado);
    }

    @Test
    void rejeitaProdutoDeOutroRestaurante() {
        when(produtoRepo.findById(101L)).thenReturn(Optional.of(produto(101L, 20L, true)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> produtoService.validarItens(request(10L, 101L, 1)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejeitaRequisicaoIncompletaOuSemItens() {
        assertBadRequest(null);
        assertBadRequest(new ValidacaoItensRequestDTO(null, List.of(new ItemValidacaoRequestDTO(101L, 1))));
        assertBadRequest(new ValidacaoItensRequestDTO(10L, null));
        assertBadRequest(new ValidacaoItensRequestDTO(10L, List.of()));
    }

    @Test
    void rejeitaItemSemIdentificadorOuQuantidadePositiva() {
        assertBadRequest(new ValidacaoItensRequestDTO(10L, List.of(new ItemValidacaoRequestDTO(null, 1))));
        assertBadRequest(new ValidacaoItensRequestDTO(10L, List.of(new ItemValidacaoRequestDTO(101L, null))));
        assertBadRequest(new ValidacaoItensRequestDTO(10L, List.of(new ItemValidacaoRequestDTO(101L, 0))));
        assertBadRequest(new ValidacaoItensRequestDTO(10L, Collections.singletonList(null)));
    }

    private void assertBadRequest(ValidacaoItensRequestDTO request) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> produtoService.validarItens(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private static ValidacaoItensRequestDTO request(Long restauranteId, Long itemId, Integer quantidade) {
        return new ValidacaoItensRequestDTO(
                restauranteId,
                List.of(new ItemValidacaoRequestDTO(itemId, quantidade)));
    }

    private static Produto produto(Long id, Long restauranteId, boolean disponivel) {
        Cardapio cardapio = new Cardapio();
        cardapio.setRestauranteId(restauranteId);

        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome("Pizza Calabresa");
        produto.setPreco(4590L);
        produto.setDisponivel(disponivel);
        produto.setCardapio(cardapio);
        return produto;
    }
}
