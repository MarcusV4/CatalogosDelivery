package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import br.upe.catalogosdelivery.cardapio.repositories.ProdutoRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProdutoServiceJpaTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private ProdutoRepo produtoRepo;

    @Autowired
    private CardapioRepo cardapioRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void listaProdutosSomenteDoRestauranteInformado() {
        Cardapio cardapioDoRestaurante10 = cardapio(10L);
        Cardapio cardapioDoRestaurante20 = cardapio(20L);
        Produto produtoDoRestaurante10 = produto("Pizza Recife", cardapioDoRestaurante10, CategoriaProduto.PIZZA);
        produto("Açaí Olinda", cardapioDoRestaurante20, CategoriaProduto.SOBREMESA);

        ProdutoService produtoService = new ProdutoService();
        produtoService.produtoRepo = produtoRepo;

        List<ProdutoResponseDTO> resultado = produtoService.listarProdutosPorRestaurante(10L);

        assertEquals(List.of(produtoDoRestaurante10.getId()), resultado.stream().map(ProdutoResponseDTO::id).toList());
    }

    @Test
    void persisteCategoriaPeloNomeDoEnum() {
        Cardapio cardapio = cardapio(10L);
        Produto produto = produto("Prato oriental", cardapio, CategoriaProduto.ORIENTAL);

        String categoriaPersistida = jdbcTemplate.queryForObject(
                "select categoria from produto where id = ?", String.class, produto.getId());

        assertEquals("ORIENTAL", categoriaPersistida);
    }

    private Cardapio cardapio(Long restauranteId) {
        Cardapio cardapio = new Cardapio();
        cardapio.setRestauranteId(restauranteId);
        return cardapioRepo.saveAndFlush(cardapio);
    }

    private Produto produto(String nome, Cardapio cardapio, CategoriaProduto categoria) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setPreco(new BigDecimal("19.90"));
        produto.setDisponivel(true);
        produto.setCategoria(categoria);
        produto.setCardapio(cardapio);
        return produtoRepo.saveAndFlush(produto);
    }
}
