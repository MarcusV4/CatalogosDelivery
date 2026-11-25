package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import br.upe.catalogosdelivery.cardapio.mappers.ProdutoMapper;
import br.upe.catalogosdelivery.cardapio.services.AdicionalService;
import br.upe.catalogosdelivery.cardapio.services.CardapioService;
import br.upe.catalogosdelivery.cardapio.services.ProdutoService;
import br.upe.catalogosdelivery.common.excecoes.GlobalExceptionHandler;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import br.upe.catalogosdelivery.common.seguranca.IdentidadeClient;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ProdutoController.class, CardapioController.class, AdicionalController.class, CategoriaController.class})
@Import({GlobalExceptionHandler.class, ProdutoMapper.class})
class CardapioEndpointsTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProdutoService produtoService;
    @MockitoBean AdicionalService adicionalService;
    @MockitoBean CardapioService cardapioService;
    @MockitoBean IdentidadeClient identidade;

    private static final String TOKEN = "Bearer token-do-dono";

    private static final String PRODUTO_JSON = """
            {"nome":"Pizza","descricao":"Massa fina","preco":2590,"disponivel":true,
             "urlImagem":"https://exemplo.test/pizza.png","categoria":"PIZZA"}""";
    private static final String ADICIONAL_JSON = """
            {"nome":"Queijo","preco":350,"disponivel":true}""";

    @BeforeEach
    void identidadeAceitaOTokenDoDono() {
        when(identidade.validarToken("token-do-dono")).thenReturn(new UsuarioAutenticado(42L, "LOJISTA"));
    }

    private static ProdutoResponseDTO produto() {
        return new ProdutoResponseDTO(7L, "Pizza", "Massa fina", 2590L, true,
                CategoriaProduto.PIZZA, 10L, "https://exemplo.test/pizza.png",
                List.of(new AdicionalResponseDTO(3L, "Queijo", 350L, true)));
    }

    @Test
    void consultaCardapioPeloRestaurante() throws Exception {
        when(produtoService.listarProdutosPorRestaurante(10L)).thenReturn(List.of(produto()));
        mvc.perform(get("/api/restaurantes/10/cardapio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restauranteId").value(10))
                .andExpect(jsonPath("$[0].adicionais[0].nome").value("Queijo"));
    }

    @Test
    void consultaProdutoPorIdE404ParaAusente() throws Exception {
        when(produtoService.buscarProduto(7L)).thenReturn(produto());
        when(produtoService.buscarProduto(99L))
                .thenThrow(new RecursoNaoEncontradoException("Produto 99 não encontrado."));
        mvc.perform(get("/api/produtos/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlImagem").value("https://exemplo.test/pizza.png"));
        mvc.perform(get("/api/produtos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(true));
    }

    @Test
    void cadastraProdutoPelaRotaNovaEPelaAntiga() throws Exception {
        when(produtoService.cadastrarProduto(any(), eq(10L))).thenReturn(produto());
        for (String rota : List.of("/api/restaurantes/10/cardapio", "/api/produtos/10")) {
            mvc.perform(post(rota).header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON).content(PRODUTO_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(7));
        }
    }

    @Test
    void atualizaProdutoPorPutEPatch() throws Exception {
        when(produtoService.atualizarProduto(eq(7L), any())).thenReturn(produto());
        when(produtoService.atualizarParcialmenteProduto(eq(7L), any())).thenReturn(produto());
        mvc.perform(put("/api/produtos/7").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON).content(PRODUTO_JSON))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/produtos/7").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preco\":3000}"))
                .andExpect(status().isOk());
    }

    @Test
    void alteraDisponibilidadeEExcluiProduto() throws Exception {
        when(produtoService.alterarDisponibilidade(7L, false)).thenReturn(produto());
        mvc.perform(patch("/api/produtos/7/disponibilidade").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponivel\":false}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/produtos/7").header("Authorization", TOKEN)).andExpect(status().isNoContent());
        verify(produtoService).excluirProduto(7L);
    }

    @Test
    void validaEntradaDeProduto() throws Exception {
        mvc.perform(put("/api/produtos/7").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"preco\":-1,\"disponivel\":true,\"categoria\":\"PIZZA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(true));
        verify(produtoService, never()).atualizarProduto(any(), any());
    }

    @Test
    void listaCategoriasDoEnum() throws Exception {
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("LANCHE"))
                .andExpect(jsonPath("$[2]").value("PIZZA"));
    }

    @Test
    void criaEListaAdicionais() throws Exception {
        var resposta = new AdicionalResponseDTO(3L, "Queijo", 350L, true);
        when(adicionalService.criar(eq(7L), any())).thenReturn(resposta);
        when(adicionalService.listar(7L)).thenReturn(List.of(resposta));
        mvc.perform(post("/api/produtos/7/adicionais").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content(ADICIONAL_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.preco").value(350));
        mvc.perform(get("/api/produtos/7/adicionais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }

    @Test
    void atualizaAdicionalPorPutEPatch() throws Exception {
        var resposta = new AdicionalResponseDTO(3L, "Queijo", 350L, true);
        when(adicionalService.atualizar(eq(7L), eq(3L), any())).thenReturn(resposta);
        when(adicionalService.atualizarParcialmente(eq(7L), eq(3L), any())).thenReturn(resposta);
        mvc.perform(put("/api/produtos/7/adicionais/3").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content(ADICIONAL_JSON))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/produtos/7/adicionais/3").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Queijo extra\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void alteraDisponibilidadeEExcluiAdicional() throws Exception {
        when(adicionalService.alterarDisponibilidade(7L, 3L, false))
                .thenReturn(new AdicionalResponseDTO(3L, "Queijo", 350L, false));
        mvc.perform(patch("/api/produtos/7/adicionais/3/disponibilidade").header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"disponivel\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(false));
        mvc.perform(delete("/api/produtos/7/adicionais/3").header("Authorization", TOKEN)).andExpect(status().isNoContent());
        verify(adicionalService).excluir(7L, 3L);
    }

    @Test
    void validaNomeEPrecoDoAdicional() throws Exception {
        mvc.perform(post("/api/produtos/7/adicionais").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"preco\":-1,\"disponivel\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(true));
        verify(adicionalService, never()).criar(any(), any());
    }

    @Test
    void retorna404ParaAdicionalDeOutroProduto() throws Exception {
        when(adicionalService.atualizar(eq(7L), eq(99L), any()))
                .thenThrow(new RecursoNaoEncontradoException("Adicional não encontrado."));
        mvc.perform(put("/api/produtos/7/adicionais/99").header("Authorization", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content(ADICIONAL_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(true));
    }

    @Test
    void escritaSemTokenRetorna401SemChamarService() throws Exception {
        mvc.perform(post("/api/restaurantes/10/cardapio").contentType(MediaType.APPLICATION_JSON).content(PRODUTO_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(true));
        mvc.perform(delete("/api/produtos/7/adicionais/3")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/cardapios/restaurante/10")).andExpect(status().isUnauthorized());
        verifyNoInteractions(produtoService, adicionalService, cardapioService, identidade);
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        when(identidade.validarToken("vencido")).thenThrow(new NaoAutenticadoException("Token inválido ou expirado."));
        mvc.perform(patch("/api/produtos/7/disponibilidade").header("Authorization", "Bearer vencido")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"disponivel\":false}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado."));
        verifyNoInteractions(produtoService);
    }

    @Test
    void identidadeForaDoArRetorna503() throws Exception {
        when(identidade.validarToken("token-do-dono"))
                .thenThrow(new ServicoIndisponivelException("Serviço de identidade indisponível", null));
        mvc.perform(delete("/api/produtos/7").header("Authorization", TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Serviço de identidade indisponível"));
        verifyNoInteractions(produtoService);
    }

    @Test
    void naoDonoRecebe403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não é proprietário do restaurante."))
                .when(produtoService).excluirProduto(7L);
        mvc.perform(delete("/api/produtos/7").header("Authorization", TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Usuário não é proprietário do restaurante."));
    }

    @Test
    void leiturasEValidacaoDoCheckoutNaoExigemToken() throws Exception {
        when(produtoService.validarItens(any())).thenReturn(List.of());
        mvc.perform(get("/api/produtos")).andExpect(status().isOk());
        mvc.perform(post("/api/produtos/validacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restauranteId\":10,\"itens\":[{\"itemId\":7,\"quantidade\":1}]}"))
                .andExpect(status().isOk());
        verifyNoInteractions(identidade);
    }
}
