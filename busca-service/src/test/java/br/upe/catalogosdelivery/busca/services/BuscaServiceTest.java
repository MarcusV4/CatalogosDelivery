package br.upe.catalogosdelivery.busca.services;

import br.upe.catalogosdelivery.busca.clients.CardapioClient;
import br.upe.catalogosdelivery.busca.clients.RestauranteClient;
import br.upe.catalogosdelivery.busca.dto.FiltroItens;
import br.upe.catalogosdelivery.busca.dto.FiltroRestaurantes;
import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.PaginaDTO;
import br.upe.catalogosdelivery.busca.dto.ProdutoDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BuscaServiceTest {

    private RestauranteClient restauranteClient;
    private CardapioClient cardapioClient;
    private BuscaService buscaService;

    private final RestauranteDTO pizzaria = new RestauranteDTO(1L, "Pizzaria do João", null, "Recife", "PE",
            null, "ABERTO", "PIZZARIA", 500L, 30, 45, true);
    private final RestauranteDTO acaiteria = new RestauranteDTO(2L, "Açaí da Praia", null, "Olinda", "PE",
            null, "FECHADO", "ACAI", 300L, 10, 20, false);
    private final RestauranteDTO hamburgueria = new RestauranteDTO(3L, "Burger Recife", null, "Recife", "PE",
            null, "ABERTO", "HAMBURGUERIA", 0L, null, null, false);

    private final ProdutoDTO calabresa = new ProdutoDTO(10L, "Pizza Calabresa", null, 4590L, true, "PIZZA", 1L, "img");
    private final ProdutoDTO marguerita = new ProdutoDTO(11L, "Pizza Marguerita", null, 4200L, false, "PIZZA", 1L, null);
    private final ProdutoDTO acai = new ProdutoDTO(20L, "Açaí 500ml", null, 1800L, true, "SOBREMESA", 2L, null);
    private final ProdutoDTO agua = new ProdutoDTO(21L, "Água Mineral", null, 350L, true, "BEBIDA", 2L, null);

    @BeforeEach
    void setUp() {
        restauranteClient = mock(RestauranteClient.class);
        cardapioClient = mock(CardapioClient.class);
        buscaService = new BuscaService(restauranteClient, cardapioClient);
    }

    // ---------- restaurantes ----------

    @Test
    void buscaRestaurantePorNomeIgnorandoAcentoEMaiusculas() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(List.of(acaiteria), restaurantes(new FiltroRestaurantes("ACAI", null, null, null)));
    }

    @Test
    void filtraRestaurantesPorCategoriaEStatus() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(List.of(pizzaria), restaurantes(new FiltroRestaurantes(null, "pizzaria", "aberto", null)));
        assertEquals(List.of(), restaurantes(new FiltroRestaurantes(null, "pizzaria", "FECHADO", null)));
    }

    @Test
    void filtraRestaurantesAbertosAgora() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria, hamburgueria));

        assertEquals(List.of(pizzaria), restaurantes(new FiltroRestaurantes(null, null, null, true)));
        // status ABERTO mas fora do horário também conta como fechado agora
        assertEquals(List.of(acaiteria, hamburgueria), restaurantes(new FiltroRestaurantes(null, null, null, false)));
        // combinado com outros filtros: "Burger Recife" está ABERTO, mas não aberto agora
        assertEquals(List.of(), restaurantes(new FiltroRestaurantes("burger", null, "ABERTO", true)));
        assertEquals(List.of(hamburgueria), restaurantes(new FiltroRestaurantes("burger", null, "ABERTO", false)));
    }

    @Test
    void ordenaRestaurantesPorNomePorPadraoIgnorandoAcento() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, hamburgueria, acaiteria));

        assertEquals(List.of(acaiteria, hamburgueria, pizzaria), restaurantes(FiltroRestaurantes.vazio()));
    }

    @Test
    void ordenaRestaurantesPorTaxaDecrescenteETempoComNulosNoFim() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, hamburgueria, acaiteria));

        assertEquals(List.of(pizzaria, acaiteria, hamburgueria),
                buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 0, 20, "taxaEntrega,desc").conteudo());
        assertEquals(List.of(acaiteria, pizzaria, hamburgueria),
                buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 0, 20, "tempoEstimado").conteudo());
        assertEquals(List.of(pizzaria, acaiteria, hamburgueria),
                buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 0, 20, "tempoEstimado,desc").conteudo());
    }

    @Test
    void paginaRestaurantes() {
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, hamburgueria, acaiteria));

        PaginaDTO<RestauranteDTO> primeira = buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 0, 2, null);
        PaginaDTO<RestauranteDTO> segunda = buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 1, 2, null);
        PaginaDTO<RestauranteDTO> alemDoFim = buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 5, 2, null);

        assertEquals(List.of(acaiteria, hamburgueria), primeira.conteudo());
        assertEquals(3, primeira.totalElementos());
        assertEquals(2, primeira.totalPaginas());
        assertEquals(List.of(pizzaria), segunda.conteudo());
        assertEquals(1, segunda.pagina());
        assertEquals(List.of(), alemDoFim.conteudo());
        assertEquals(3, alemDoFim.totalElementos());
    }

    // ---------- itens ----------

    @Test
    void buscaPratoPorNomeComDadosDoRestaurante() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, marguerita, acai));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        List<ItemBuscaDTO> resultado = itens(new FiltroItens("pizza", null, null, null, true, null));

        assertEquals(1, resultado.size());
        ItemBuscaDTO item = resultado.get(0);
        assertEquals("Pizza Calabresa", item.nome());
        assertEquals(4590L, item.preco());
        assertTrue(item.disponivel());
        assertEquals("img", item.urlImagem());
        assertEquals("Pizzaria do João", item.restauranteNome());
        assertEquals("ABERTO", item.restauranteStatus());
        assertTrue(item.restauranteAbertoAgora());
    }

    @Test
    void filtraItensPorRestauranteECategoria() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, marguerita, acai));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(2, itens(new FiltroItens(null, "PIZZA", null, null, null, 1L)).size());
        assertEquals(List.of(), itens(new FiltroItens(null, "PIZZA", null, null, null, 2L)));
    }

    @Test
    void filtraItensPorFaixaDePrecoInclusiva() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, marguerita, acai, agua));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(List.of("Açaí 500ml", "Pizza Marguerita"),
                nomes(itens(new FiltroItens(null, null, 1800L, 4200L, null, null))));
        assertEquals(List.of("Água Mineral"), nomes(itens(new FiltroItens(null, null, null, 350L, null, null))));
        assertEquals(List.of("Pizza Calabresa"), nomes(itens(new FiltroItens(null, null, 4500L, null, null, null))));
    }

    @Test
    void combinaFiltrosDeItens() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, marguerita, acai, agua));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(List.of("Pizza Calabresa"),
                nomes(itens(new FiltroItens("pizza", "pizza", 4000L, 5000L, true, 1L))));
    }

    @Test
    void ordenaItensPorPreco() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, marguerita, acai, agua));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria, acaiteria));

        assertEquals(List.of("Água Mineral", "Açaí 500ml", "Pizza Marguerita", "Pizza Calabresa"),
                nomes(buscaService.buscarItens(FiltroItens.vazio(), 0, 20, "preco").conteudo()));
        assertEquals(List.of("Pizza Calabresa", "Pizza Marguerita", "Açaí 500ml", "Água Mineral"),
                nomes(buscaService.buscarItens(FiltroItens.vazio(), 0, 20, "preco,DESC").conteudo()));
    }

    @Test
    void escondeItensDeRestauranteExcluido() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(calabresa, acai));
        when(restauranteClient.listarRestaurantes()).thenReturn(List.of(pizzaria)); // restaurante 2 foi desativado

        assertEquals(List.of("Pizza Calabresa"), nomes(itens(FiltroItens.vazio())));
    }

    @Test
    void buscaDeItensContinuaSemRestaurantes() {
        when(cardapioClient.listarProdutos()).thenReturn(List.of(acai));
        when(restauranteClient.listarRestaurantes())
                .thenThrow(new ServicoIndisponivelException("fora do ar", null));

        List<ItemBuscaDTO> resultado = itens(new FiltroItens("açaí", null, null, null, null, null));

        assertEquals(1, resultado.size());
        assertNull(resultado.get(0).restauranteNome());
        assertNull(resultado.get(0).restauranteAbertoAgora());
    }

    @Test
    void semCardapioABuscaDeItensFalhaComServicoIndisponivel() {
        when(cardapioClient.listarProdutos())
                .thenThrow(new ServicoIndisponivelException("fora do ar", null));

        assertThrows(ServicoIndisponivelException.class,
                () -> itens(new FiltroItens("pizza", null, null, null, null, null)));
    }

    // ---------- validação ----------

    @Test
    void recusaParametrosInvalidosSemChamarOutrosServicos() {
        assertBadRequest(() -> buscaService.buscarItens(new FiltroItens(null, null, 3000L, 2000L, null, null), 0, 20, null));
        assertBadRequest(() -> buscaService.buscarItens(new FiltroItens(null, null, -1L, null, null, null), 0, 20, null));
        assertBadRequest(() -> buscaService.buscarItens(FiltroItens.vazio(), -1, 20, null));
        assertBadRequest(() -> buscaService.buscarItens(FiltroItens.vazio(), 0, 0, null));
        assertBadRequest(() -> buscaService.buscarItens(FiltroItens.vazio(), 0, BuscaService.TAMANHO_MAXIMO + 1, null));
        assertBadRequest(() -> buscaService.buscarItens(FiltroItens.vazio(), 0, 20, "restauranteNome"));
        assertBadRequest(() -> buscaService.buscarItens(FiltroItens.vazio(), 0, 20, "preco,cima"));
        assertBadRequest(() -> buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 0, 20, "preco"));
        assertBadRequest(() -> buscaService.buscarRestaurantes(FiltroRestaurantes.vazio(), 0, 20, "nome,asc,x"));
        verifyNoInteractions(restauranteClient, cardapioClient);
    }

    @Test
    void normalizarRemoveAcentosEMaiusculas() {
        assertEquals("acai", BuscaService.normalizar("  AÇAÍ "));
        assertEquals("pizza calabresa", BuscaService.normalizar("Pizza Calabresa"));
    }

    private List<RestauranteDTO> restaurantes(FiltroRestaurantes filtro) {
        return buscaService.buscarRestaurantes(filtro, 0, 20, null).conteudo();
    }

    private List<ItemBuscaDTO> itens(FiltroItens filtro) {
        return buscaService.buscarItens(filtro, 0, 20, null).conteudo();
    }

    private static List<String> nomes(List<ItemBuscaDTO> itens) {
        return itens.stream().map(ItemBuscaDTO::nome).toList();
    }

    private static void assertBadRequest(org.junit.jupiter.api.function.Executable chamada) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, chamada).getStatusCode());
    }
}
