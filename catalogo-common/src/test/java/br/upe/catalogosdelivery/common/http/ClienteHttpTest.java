package br.upe.catalogosdelivery.common.http;

import br.upe.catalogosdelivery.common.excecoes.FalhaIntegracaoException;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClienteHttpTest {

    private static final String BASE = "http://restaurantes:8004";

    private MockRestServiceServer servidor;
    private ClienteHttp cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ClienteHttp("Teste", "Restaurantes", BASE, builder.build());
    }

    @Test
    void retornaCorpoQuandoSucesso() {
        servidor.expect(requestTo(BASE + "/api/restaurantes/10"))
                .andRespond(withSuccess("{\"restauranteNome\":\"Pizzaria\"}", MediaType.APPLICATION_JSON));

        Map<?, ?> resposta = cliente.get("/api/restaurantes/{id}", Map.class, "não encontrado", 10);

        assertEquals("Pizzaria", resposta.get("restauranteNome"));
    }

    @Test
    void notFoundViraRecursoNaoEncontrado() {
        servidor.expect(requestTo(BASE + "/api/restaurantes/99")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        RecursoNaoEncontradoException e = assertThrows(RecursoNaoEncontradoException.class,
                () -> cliente.get("/api/restaurantes/{id}", Map.class, "Restaurante 99 não encontrado.", 99));
        assertEquals("Restaurante 99 não encontrado.", e.getMessage());
    }

    @Test
    void erro5xxViraServicoIndisponivel() {
        servidor.expect(requestTo(BASE + "/api/restaurantes")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ServicoIndisponivelException.class,
                () -> cliente.get("/api/restaurantes", Map.class, "x"));
    }

    @Test
    void falhaDeConexaoViraServicoIndisponivel() {
        servidor.expect(requestTo(BASE + "/api/restaurantes")).andRespond(withException(new IOException("Connection refused")));

        ServicoIndisponivelException e = assertThrows(ServicoIndisponivelException.class,
                () -> cliente.get("/api/restaurantes", Map.class, "x"));
        assertEquals("O serviço de Restaurantes está indisponível no momento. Tente novamente mais tarde.", e.getMessage());
    }

    @Test
    void outroErro4xxViraFalhaIntegracao() {
        servidor.expect(requestTo(BASE + "/api/restaurantes")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(FalhaIntegracaoException.class,
                () -> cliente.get("/api/restaurantes", Map.class, "x"));
    }

    @Test
    void deleteComSucesso() {
        servidor.expect(requestTo(BASE + "/api/cardapios/restaurante/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> cliente.delete("/api/cardapios/restaurante/{id}", "x", 1));
        servidor.verify();
    }

    @Test
    void deleteComServicoForaDoArViraServicoIndisponivel() {
        servidor.expect(requestTo(BASE + "/api/cardapios/restaurante/1"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThrows(ServicoIndisponivelException.class,
                () -> cliente.delete("/api/cardapios/restaurante/{id}", "x", 1));
    }
}
