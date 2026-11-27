package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoPatchDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoCrudServiceTest {
    @Mock ProdutoRepo produtoRepo;
    @Mock CardapioRepo cardapioRepo;
    @Mock RestauranteClient restauranteClient;
    @Mock AutorizacaoRestauranteService autorizacaoRestauranteService;
    @InjectMocks ProdutoService service;

    private Produto produto;

    @BeforeEach
    void preparar() {
        Cardapio cardapio = new Cardapio();
        cardapio.setRestauranteId(10L);
        produto = new Produto();
        produto.setId(7L);
        produto.setNome("Pizza");
        produto.setPreco(2590L);
        produto.setCardapio(cardapio);
    }

    @Test
    void consultaProdutoEDevolveRestauranteImagemEAdicionais() {
        produto.setUrlImagem("https://exemplo.test/pizza.png");
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));

        var resposta = service.buscarProduto(7L);

        assertEquals(10L, resposta.restauranteId());
        assertEquals(produto.getUrlImagem(), resposta.urlImagem());
        assertEquals(0, resposta.adicionais().size());
    }

    @Test
    void atualizaProdutoEInvocaPontoDePermissao() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));
        when(produtoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service.atualizarProduto(7L,
                new ProdutoRequestDTO("Pizza nova", "Descrição", 3000L, true, "imagem", CategoriaProduto.PIZZA));

        assertEquals("Pizza nova", resposta.nome());
        assertEquals(3000L, resposta.preco());
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void atualizacaoParcialPreservaCamposNaoInformadosEInvocaPermissao() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));
        when(produtoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = service.atualizarParcialmenteProduto(7L,
                new ProdutoPatchDTO(null, null, 2700L, null, null, null));

        assertEquals("Pizza", resposta.nome());
        assertEquals(2700L, resposta.preco());
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void alteraDisponibilidadeEInvocaPermissao() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));
        when(produtoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.alterarDisponibilidade(7L, true).disponivel());
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void excluiProdutoEInvocaPermissao() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));

        service.excluirProduto(7L);

        verify(produtoRepo).delete(produto);
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void produtoInexistenteOuDeCardapioExcluidoRetorna404() {
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarProduto(7L));
        produto.getCardapio().setExcluidoEm(Instant.parse("2026-11-16T12:00:00Z"));
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.excluirProduto(7L));
        verifyNoInteractions(autorizacaoRestauranteService);
    }

    @Test
    void cadastroNaoReabreCardapioExcluido() {
        produto.getCardapio().setExcluidoEm(Instant.parse("2026-11-16T12:00:00Z"));
        when(cardapioRepo.findByRestauranteId(10L)).thenReturn(Optional.of(produto.getCardapio()));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.cadastrarProduto(produto, 10L));
        // A permissão é conferida antes de olhar o cardápio.
        verify(autorizacaoRestauranteService).validarDono(any());
        verify(produtoRepo, never()).save(any());
    }
}
