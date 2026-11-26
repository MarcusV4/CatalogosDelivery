
package br.upe.catalogosdelivery.cardapio.integration;

import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class ProdutoApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardapioRepo cardapioRepo;

    @Autowired
    private ProdutoRepo produtoRepo;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    @Transactional
    void validaProdutoDisponivelComSucessoERegistraAConsultaDoCheckout(CapturedOutput saida) throws Exception {
        Produto produto = criarProduto(10L, true);

        mockMvc.perform(post("/api/produtos/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "restauranteId": 10,
                                    "itens": [
                                        {
                                            "itemId": %d,
                                            "quantidade": 2
                                        }
                                    ]
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(produto.getId()))
                .andExpect(jsonPath("$[0].existe").value(true))
                .andExpect(jsonPath("$[0].disponivel").value(true))
                .andExpect(jsonPath("$[0].preco").value(4590))
                .andExpect(jsonPath("$[0].restauranteId").value(10))
                .andExpect(jsonPath("$[0].nome").value("Pizza Calabresa"));

        assertTrue(saida.getOut().contains(
                "[Cardápio] Checkout consultou itens [%d] do restaurante 10".formatted(produto.getId())));
    }

    @Test
    @Transactional
    void retornaItemInexistente() throws Exception {
        mockMvc.perform(post("/api/produtos/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "restauranteId": 10,
                                    "itens": [
                                        {
                                            "itemId": 99999,
                                            "quantidade": 1
                                        }
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].existe").value(false))
                .andExpect(jsonPath("$[0].disponivel").value(false))
                .andExpect(jsonPath("$[0].preco").doesNotExist());
    }

    @Test
    @Transactional
    void retornaProdutoIndisponivel() throws Exception {
        Produto produto = criarProduto(10L, false);

        mockMvc.perform(post("/api/produtos/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "restauranteId": 10,
                                    "itens": [
                                        {
                                            "itemId": %d,
                                            "quantidade": 1
                                        }
                                    ]
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].existe").value(true))
                .andExpect(jsonPath("$[0].disponivel").value(false));
    }

    @Test
    @Transactional
    void rejeitaProdutoDeOutroRestaurante() throws Exception {
        Produto produto = criarProduto(20L, true);

        mockMvc.perform(post("/api/produtos/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "restauranteId": 10,
                                    "itens": [
                                        {
                                            "itemId": %d,
                                            "quantidade": 1
                                        }
                                    ]
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isBadRequest());
    }

    private Produto criarProduto(Long restauranteId, boolean disponivel) {
        Cardapio cardapio = new Cardapio();
        cardapio.setRestauranteId(restauranteId);
        cardapio = cardapioRepo.saveAndFlush(cardapio);

        Produto produto = new Produto();
        produto.setNome("Pizza Calabresa");
        produto.setDescricao("Pizza de teste");
        produto.setPreco(4590L);
        produto.setDisponivel(disponivel);
        produto.setCardapio(cardapio);

        return produtoRepo.saveAndFlush(produto);
    }
}
