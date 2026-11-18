package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.entities.Adicional;
import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalPatchDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalRequestDTO;
import br.upe.catalogosdelivery.cardapio.repositories.AdicionalRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdicionalServiceTest {
    @Mock AdicionalRepo adicionalRepo;
    @Mock ProdutoRepo produtoRepo;
    @Mock AutorizacaoRestauranteService autorizacaoRestauranteService;
    @InjectMocks ProdutoService produtoService;
    AdicionalService adicionalService;

    private Produto produto;
    private Adicional adicional;

    @BeforeEach
    void preparar() {
        adicionalService = new AdicionalService(adicionalRepo, produtoService);
        Cardapio cardapio = new Cardapio();
        cardapio.setRestauranteId(10L);
        produto = new Produto();
        produto.setId(7L);
        produto.setCardapio(cardapio);
        adicional = new Adicional();
        adicional.setId(3L);
        adicional.setNome("Queijo");
        adicional.setPreco(350L);
        adicional.setProduto(produto);
    }

    @Test
    void listaAdicionaisDoProduto() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));
        when(adicionalRepo.findByProduto_IdOrderById(7L)).thenReturn(List.of(adicional));

        assertEquals("Queijo", adicionalService.listar(7L).getFirst().nome());
    }

    @Test
    void criaAdicionalEInvocaPontoDePermissao() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));
        when(adicionalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = adicionalService.criar(7L, new AdicionalRequestDTO("Bacon", 500L, true));

        assertEquals("Bacon", resposta.nome());
        assertEquals(500L, resposta.preco());
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void atualizaAdicionalEInvocaPermissao() {
        prepararAdicional();
        when(adicionalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = adicionalService.atualizar(7L, 3L,
                new AdicionalRequestDTO("Queijo duplo", 700L, true));

        assertEquals("Queijo duplo", resposta.nome());
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void atualizaParcialmenteEInvocaPermissao() {
        prepararAdicional();
        when(adicionalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resposta = adicionalService.atualizarParcialmente(7L, 3L,
                new AdicionalPatchDTO(null, 400L, null));

        assertEquals("Queijo", resposta.nome());
        assertEquals(400L, resposta.preco());
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void alteraDisponibilidadeEInvocaPermissao() {
        prepararAdicional();
        when(adicionalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(adicionalService.alterarDisponibilidade(7L, 3L, true).disponivel());
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void excluiAdicionalEInvocaPermissao() {
        prepararAdicional();

        adicionalService.excluir(7L, 3L);

        verify(adicionalRepo).delete(adicional);
        verify(autorizacaoRestauranteService).validarPermissao(10L);
    }

    @Test
    void adicionalDeOutroProdutoRetorna404() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> adicionalService.excluir(7L, 99L));
        verifyNoInteractions(autorizacaoRestauranteService);
    }

    @Test
    void produtoInexistenteOuCardapioExcluidoImpedeEscrita() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> adicionalService.criar(7L, new AdicionalRequestDTO("Queijo", 350L, true)));
        produto.getCardapio().setExcluidoEm(Instant.parse("2026-11-16T12:00:00Z"));
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));

        assertThrows(RecursoNaoEncontradoException.class,
                () -> adicionalService.criar(7L, new AdicionalRequestDTO("Queijo", 350L, true)));
        verifyNoInteractions(autorizacaoRestauranteService, adicionalRepo);
    }

    private void prepararAdicional() {
        when(produtoRepo.findById(7L)).thenReturn(Optional.of(produto));
        when(adicionalRepo.findByIdAndProduto_Id(3L, 7L)).thenReturn(Optional.of(adicional));
    }
}
