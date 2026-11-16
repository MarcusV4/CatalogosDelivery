package br.upe.catalogosdelivery.busca.services;

import br.upe.catalogosdelivery.busca.clients.CardapioClient;
import br.upe.catalogosdelivery.busca.clients.RestauranteClient;
import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.ProdutoDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscaServiceTest {

    private RestauranteClient restauranteClient;
    private CardapioClient cardapioClient;
    private BuscaService buscaService;

    private final RestauranteDTO pizzaria = new RestauranteDTO(1L, "Pizzaria do João", null, "Recife", "PE",
            null, "ABERTO", "PIZZARIA", 5f);
    private final RestauranteDTO acaiteria = new RestauranteDTO(2L, "Açaí da Praia", null, "Olinda", "PE",
            null, "FECHADO", "AÇAÍ", 3f);

    private final ProdutoDTO calabresa = new ProdutoDTO(10L, "Pizza Calabresa", null, new BigDecimal("45.90"), true, "PIZZA", 1L);
    private final ProdutoDTO marguerita = new ProdutoDTO(11L, "Pizza Marguerita", null, new BigDecimal("42.00"), false, "PIZZA", 1L);
    private final ProdutoDTO acai = new ProdutoDTO(20L, "Açaí 500ml", null, new BigDecimal("18.00"), true, "SOBREMESA", 2L);

    @BeforeEach
    void setUp() {
        restauranteClient = mock(RestauranteClient.class);
        cardapioClient = mock(CardapioClient.class);
        buscaService = new BuscaService(restauranteClient, cardapioClient);
    }

    @Test
    void buscaRestaurantePorNomeIgnorandoAcentoEMaiusculas() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        List<RestauranteDTO> resultado = buscaService.buscarRestaurantes("ACAI", null, null);

        assertEquals(List.of(acaiteria), resultado);
    }

    @Test
    void filtraRestaurantesPorCategoriaEStatus() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(List.of(pizzaria), buscaService.buscarRestaurantes(null, "pizzaria", "aberto"));
        assertEquals(List.of(), buscaService.buscarRestaurantes(null, "pizzaria", "FECHADO"));
    }

    @Test
    void buscaPratoPorNomeComDadosDoRestaurante() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, marguerita, acai));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        List<ItemBuscaDTO> resultado = buscaService.buscarItens("pizza", null, null, true);

        assertEquals(1, resultado.size());
        assertEquals("Pizza Calabresa", resultado.get(0).nome());
        assertEquals("Pizzaria do João", resultado.get(0).restauranteNome());
        assertEquals("ABERTO", resultado.get(0).restauranteStatus());
    }

    @Test
    void filtraItensPorRestauranteECategoria() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, marguerita, acai));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(2, buscaService.buscarItens(null, "PIZZA", 1L, null).size());
        assertEquals(List.of(), buscaService.buscarItens(null, "PIZZA", 2L, null));
    }

    @Test
    void escondeItensDeRestauranteExcluido() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, acai));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria)); // restaurante 2 foi excluído

        List<ItemBuscaDTO> resultado = buscaService.buscarItens(null, null, null, null);

        assertEquals(1, resultado.size());
        assertEquals("Pizza Calabresa", resultado.get(0).nome());
    }

    @Test
    void buscaDeItensContinuaSemRestaurantes() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(acai));
        when(restauranteClient.listarRestaurantes())
                .thenThrow(new ServicoIndisponivelException("fora do ar", null));

        List<ItemBuscaDTO> resultado = buscaService.buscarItens("açaí", null, null, null);

        assertEquals(1, resultado.size());
        assertNull(resultado.get(0).restauranteNome());
    }

    @Test
    void semCardapioABuscaDeItensFalhaComServicoIndisponivel() {
        when(cardapioClient.listarProdutos())
                .thenThrow(new ServicoIndisponivelException("fora do ar", null));

        assertThrows(ServicoIndisponivelException.class,
                () -> buscaService.buscarItens("pizza", null, null, null));
    }
}
