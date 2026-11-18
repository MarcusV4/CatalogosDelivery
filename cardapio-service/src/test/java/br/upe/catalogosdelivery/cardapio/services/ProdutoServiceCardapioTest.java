package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.clients.RestauranteResumoDTO;
import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Criação do cardápio no primeiro produto, inclusive com requisições simultâneas. */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceCardapioTest {

    @Mock
    ProdutoRepo produtoRepo;

    @Mock
    CardapioRepo cardapioRepo;

    @Mock
    RestauranteClient restauranteClient;

    @Mock
    AutorizacaoRestauranteService autorizacaoRestauranteService;

    @InjectMocks
    ProdutoService produtoService;

    @Test
    void criaOCardapioNoPrimeiroProduto() {
        Cardapio criado = cardapio(10L);
        when(cardapioRepo.findByRestauranteId(10L)).thenReturn(Optional.empty());
        when(cardapioRepo.saveAndFlush(any())).thenReturn(criado);
        when(produtoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Produto produto = new Produto();
        produto.setPreco(2590L);

        produtoService.cadastrarProduto(produto, 10L);

        assertSame(criado, produto.getCardapio());
    }

    @Test
    void usaOCardapioCriadoPorUmaRequisicaoSimultanea() {
        Cardapio criadoPelaOutraRequisicao = cardapio(10L);
        when(cardapioRepo.findByRestauranteId(10L))
                .thenReturn(Optional.empty())                      // as duas requisições não encontram
                .thenReturn(Optional.of(criadoPelaOutraRequisicao)); // depois da falha, encontra o da outra
        when(cardapioRepo.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key restaurante_id"));
        when(produtoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Produto produto = new Produto();
        produto.setPreco(2590L);

        produtoService.cadastrarProduto(produto, 10L);

        assertSame(criadoPelaOutraRequisicao, produto.getCardapio());
    }

    @Test
    void naoCriaCardapioParaRestauranteInexistente() {
        when(restauranteClient.buscarRestaurante(99L))
                .thenThrow(new RecursoNaoEncontradoException("Restaurante 99 não encontrado."));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.cadastrarProduto(produtoComPreco(2590L), 99L));

        verify(cardapioRepo, never()).saveAndFlush(any());
        verify(produtoRepo, never()).save(any());
    }

    @Test
    void naoCadastraProdutoEmRestauranteDesativado() {
        when(restauranteClient.buscarRestaurante(10L))
                .thenReturn(new RestauranteResumoDTO(10L, "Pizzaria", "INDISPONIVEL", 0L));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> produtoService.cadastrarProduto(produtoComPreco(2590L), 10L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verifyNoInteractions(cardapioRepo, produtoRepo, autorizacaoRestauranteService);
    }

    @Test
    void cadastraProdutoEmRestauranteFechado() {
        when(restauranteClient.buscarRestaurante(10L))
                .thenReturn(new RestauranteResumoDTO(10L, "Pizzaria", "FECHADO", 0L));
        Cardapio existente = cardapio(10L);
        when(cardapioRepo.findByRestauranteId(10L)).thenReturn(Optional.of(existente));
        when(produtoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Produto produto = produtoComPreco(2590L);

        produtoService.cadastrarProduto(produto, 10L);

        assertSame(existente, produto.getCardapio());
    }

    @Test
    void rejeitaPrecoAusenteOuNegativoAntesDeChamarOutrosServicos() {
        for (Long preco : new Long[]{null, -1L}) {
            ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                    () -> produtoService.cadastrarProduto(produtoComPreco(preco), 10L));
            assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        }
        verifyNoInteractions(restauranteClient, cardapioRepo, produtoRepo);
    }

    private static Cardapio cardapio(Long restauranteId) {
        Cardapio cardapio = new Cardapio();
        cardapio.setIdCardapio(1L);
        cardapio.setRestauranteId(restauranteId);
        return cardapio;
    }

    private static Produto produtoComPreco(Long preco) {
        Produto produto = new Produto();
        produto.setPreco(preco);
        return produto;
    }
}
