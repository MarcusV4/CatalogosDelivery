package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItemResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItensRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.services.CardapioService;
import br.upe.catalogosdelivery.cardapio.services.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CardapioController.class, ProdutoController.class})
class CardapioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardapioService cardapioService;

    @MockitoBean
    private ProdutoService produtoService;

    @Test
    void excluiCardapioERetorna204SemCorpo() throws Exception {
        mockMvc.perform(delete("/api/cardapios/restaurante/10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cardapioService).excluirCardapioDoRestaurante(10L);
    }

    @Test
    void listaTodosOsProdutos() throws Exception {
        when(produtoService.listarProdutos()).thenReturn(List.of(produtoDTO()));

        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].preco").value(2590));
    }

    @Test
    void listaProdutosDoRestauranteInformado() throws Exception {
        when(produtoService.listarProdutosPorRestaurante(10L)).thenReturn(List.of(produtoDTO()));

        mockMvc.perform(get("/api/produtos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restauranteId").value(10));

        verify(produtoService).listarProdutosPorRestaurante(10L);
    }

    @Test
    void cadastraProdutoERetorna201() throws Exception {
        when(produtoService.cadastrarProduto(any(Produto.class), eq(10L))).thenReturn(produtoDTO());

        mockMvc.perform(post("/api/produtos/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Pizza Recife","preco":2590,"disponivel":true,"categoria":"PIZZA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Pizza Recife"))
                .andExpect(jsonPath("$.preco").value(2590));

        verify(produtoService).cadastrarProduto(any(Produto.class), eq(10L));
    }

    @Test
    void validaItensDoCheckoutERetornaResultado() throws Exception {
        when(produtoService.validarItens(any(ValidacaoItensRequestDTO.class)))
                .thenReturn(List.of(new ValidacaoItemResponseDTO(101L, true, true, 2590L, 10L, "Pizza Recife")));

        mockMvc.perform(post("/api/produtos/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restauranteId":10,"itens":[{"itemId":101,"quantidade":2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(101))
                .andExpect(jsonPath("$[0].existe").value(true))
                .andExpect(jsonPath("$[0].preco").value(2590));

        verify(produtoService).validarItens(any(ValidacaoItensRequestDTO.class));
    }

    private static ProdutoResponseDTO produtoDTO() {
        return new ProdutoResponseDTO(101L, "Pizza Recife", "Mussarela", 2590L, true,
                CategoriaProduto.PIZZA, 10L);
    }
}
