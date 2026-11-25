package br.upe.catalogosdelivery.busca.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a Busca inteira e substitui Restaurantes e Cardápio por servidores HTTP falsos:
 * as chamadas passam pelo ClienteHttp real (log, timeout e tradução de erros para 503).
 */
@SpringBootTest(properties = "catalogo.servicos.timeout=2s")
@AutoConfigureMockMvc
class BuscaApiIntegrationTest {

    private static final String RESTAURANTES = """
            [
              {"restauranteId":1,"restauranteNome":"Pizzaria Exemplo","logradouro":"Rua A","cidade":"Recife",
               "estado":"PE","statusRestaurante":"ABERTO","categoriaRestaurante":"PIZZARIA","taxaEntrega":0,
               "tempoEstimadoMin":30,"tempoEstimadoMax":45,"ownerUserId":1,"abertoAgora":true},
              {"restauranteId":2,"restauranteNome":"Açaí Exemplo","restauranteDesc":"Tigelas e cremes gelados",
               "cidade":"Olinda","estado":"PE",
               "statusRestaurante":"FECHADO","categoriaRestaurante":"ACAI","taxaEntrega":300,
               "tempoEstimadoMin":10,"tempoEstimadoMax":20,"ownerUserId":1,"abertoAgora":false}
            ]
            """;

    private static final String PRODUTOS = """
            [
              {"id":10,"nome":"Pizza Margherita","preco":2590,"disponivel":true,"categoria":"PIZZA",
               "restauranteId":1,"urlImagem":null,
               "adicionais":[{"id":1,"nome":"Queijo extra","preco":350,"disponivel":true}]},
              {"id":11,"nome":"Pizza Calabresa","descricao":"Calabresa fatiada e cebola","preco":2890,"disponivel":true,"categoria":"PIZZA",
               "restauranteId":1,"urlImagem":"https://exemplo.test/calabresa.png","adicionais":[]},
              {"id":12,"nome":"Pizza Quatro Queijos","preco":3290,"disponivel":false,"categoria":"PIZZA",
               "restauranteId":1,"adicionais":[]},
              {"id":20,"nome":"Açaí 500 ml","preco":1800,"disponivel":true,"categoria":"SOBREMESA",
               "restauranteId":2,"adicionais":[]}
            ]
            """;

    private static final ServicoFalso restaurantes = new ServicoFalso("/api/restaurantes");
    private static final ServicoFalso cardapio = new ServicoFalso("/api/produtos");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void apontarParaServicosFalsos(DynamicPropertyRegistry registry) {
        registry.add("catalogo.servicos.restaurantes.url", restaurantes::url);
        registry.add("catalogo.servicos.cardapio.url", cardapio::url);
    }

    @AfterAll
    static void pararServicosFalsos() {
        restaurantes.parar();
        cardapio.parar();
    }

    @BeforeEach
    void servicosNoAr() {
        restaurantes.responder(200, RESTAURANTES);
        cardapio.responder(200, PRODUTOS);
    }

    @Test
    void encontraPratoPeloNomeComRestaurantePrecoEDisponibilidade() throws Exception {
        mockMvc.perform(get("/api/busca/itens").param("nome", "PIZZA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.conteudo[*].nome",
                        contains("Pizza Calabresa", "Pizza Margherita", "Pizza Quatro Queijos")))
                .andExpect(jsonPath("$.conteudo[0].preco").value(2890))
                .andExpect(jsonPath("$.conteudo[0].disponivel").value(true))
                .andExpect(jsonPath("$.conteudo[0].urlImagem").value("https://exemplo.test/calabresa.png"))
                .andExpect(jsonPath("$.conteudo[0].restauranteId").value(1))
                .andExpect(jsonPath("$.conteudo[0].restauranteNome").value("Pizzaria Exemplo"))
                .andExpect(jsonPath("$.conteudo[0].restauranteStatus").value("ABERTO"))
                .andExpect(jsonPath("$.conteudo[0].restauranteAbertoAgora").value(true));
    }

    @Test
    void ignoraAcentosNaBuscaPorNome() throws Exception {
        mockMvc.perform(get("/api/busca/itens").param("nome", "acai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Açaí 500 ml")))
                .andExpect(jsonPath("$.conteudo[0].restauranteAbertoAgora").value(false));
    }

    @Test
    void combinaFiltrosDeItensComOrdenacao() throws Exception {
        mockMvc.perform(get("/api/busca/itens")
                        .param("categoria", "pizza")
                        .param("precoMin", "2000")
                        .param("precoMax", "3000")
                        .param("disponivel", "true")
                        .param("restauranteId", "1")
                        .param("sort", "preco,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Pizza Calabresa", "Pizza Margherita")))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void buscaPelaDescricaoEFiltraEntregaGratis() throws Exception {
        mockMvc.perform(get("/api/busca/itens").param("nome", "cebola"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Pizza Calabresa")));
        mockMvc.perform(get("/api/busca/restaurantes").param("nome", "gelados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].restauranteNome", contains("Açaí Exemplo")));
        mockMvc.perform(get("/api/busca/restaurantes").param("entregaGratis", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].restauranteNome", contains("Pizzaria Exemplo")));
    }

    @Test
    void filtraItensPelosDadosDoRestaurante() throws Exception {
        mockMvc.perform(get("/api/busca/itens")
                        .param("categoriaRestaurante", "acai")
                        .param("abertoAgora", "false")
                        .param("entregaGratis", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Açaí 500 ml")));
        mockMvc.perform(get("/api/busca/itens").param("abertoAgora", "true").param("entregaGratis", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
    }

    @Test
    void filtroDoRestauranteComRestaurantesForaDoArRetorna503() throws Exception {
        restaurantes.responder(500, "");
        mockMvc.perform(get("/api/busca/itens").param("abertoAgora", "true"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem", containsString("Restaurantes está indisponível")));
    }

    @Test
    void paginaItens() throws Exception {
        mockMvc.perform(get("/api/busca/itens").param("sort", "preco").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].nome", contains("Pizza Calabresa", "Pizza Quatro Queijos")))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void filtraRestaurantesAbertosAgoraEPagina() throws Exception {
        mockMvc.perform(get("/api/busca/restaurantes").param("abertoAgora", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].restauranteNome", contains("Pizzaria Exemplo")))
                .andExpect(jsonPath("$.conteudo[0].tempoEstimadoMax").value(45));

        mockMvc.perform(get("/api/busca/restaurantes").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].restauranteNome").value("Pizzaria Exemplo"))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void restaurantesForaDoArRetorna503Amigavel() throws Exception {
        restaurantes.responder(500, "");
        mockMvc.perform(get("/api/busca/restaurantes"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem", containsString("Restaurantes está indisponível")));
    }

    @Test
    void cardapioForaDoArRetorna503Amigavel() throws Exception {
        cardapio.responder(503, "");
        mockMvc.perform(get("/api/busca/itens").param("nome", "pizza"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem", containsString("Cardápio está indisponível")));
    }

    @Test
    void itensContinuamSemOsDadosDoRestauranteQuandoRestaurantesCai() throws Exception {
        restaurantes.responder(500, "");
        mockMvc.perform(get("/api/busca/itens").param("nome", "acai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].nome").value("Açaí 500 ml"))
                .andExpect(jsonPath("$.conteudo[0].restauranteId").value(2))
                .andExpect(jsonPath("$.conteudo[0].restauranteNome").value(nullValue()))
                .andExpect(jsonPath("$.conteudo[0].restauranteAbertoAgora").value(nullValue()));
    }

    @Test
    void recusaParametrosInvalidosSemChamarOsOutrosServicos() throws Exception {
        int chamadasAntes = restaurantes.chamadas() + cardapio.chamadas();

        mockMvc.perform(get("/api/busca/itens").param("precoMin", "3000").param("precoMax", "2000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("precoMin não pode ser maior que precoMax."));
        mockMvc.perform(get("/api/busca/itens").param("precoMin", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para 'precoMin'."));
        mockMvc.perform(get("/api/busca/itens").param("size", "500"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/busca/restaurantes").param("sort", "preco"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Ordenação inválida")));

        assertEquals(chamadasAntes, restaurantes.chamadas() + cardapio.chamadas());
    }

    @Test
    void swaggerDocumentaFiltrosPaginacaoEOrdenacao() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/busca/itens'].get.parameters[*].name",
                        contains("nome", "categoria", "precoMin", "precoMax", "disponivel", "restauranteId",
                                "categoriaRestaurante", "abertoAgora", "entregaGratis", "page", "size", "sort")))
                .andExpect(jsonPath("$.paths['/api/busca/restaurantes'].get.parameters[*].name",
                        contains("nome", "categoria", "status", "abertoAgora", "entregaGratis", "page", "size", "sort")))
                .andExpect(jsonPath("$.paths['/api/busca/itens'].get.responses['503']").exists());
    }

    /** Servidor HTTP mínimo que responde sempre o mesmo status e corpo num caminho. */
    private static final class ServicoFalso {
        private final HttpServer servidor;
        private final AtomicInteger chamadas = new AtomicInteger();
        private volatile int status = 200;
        private volatile String corpo = "[]";

        ServicoFalso(String caminho) {
            try {
                servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            servidor.createContext(caminho, troca -> {
                chamadas.incrementAndGet();
                byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
                troca.getResponseHeaders().add("Content-Type", "application/json");
                troca.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
                try (OutputStream saida = troca.getResponseBody()) {
                    saida.write(bytes);
                }
            });
            servidor.start();
        }

        String url() {
            return "http://" + servidor.getAddress().getHostString() + ":" + servidor.getAddress().getPort();
        }

        void responder(int novoStatus, String novoCorpo) {
            status = novoStatus;
            corpo = novoCorpo;
        }

        int chamadas() {
            return chamadas.get();
        }

        void parar() {
            servidor.stop(0);
        }
    }
}
