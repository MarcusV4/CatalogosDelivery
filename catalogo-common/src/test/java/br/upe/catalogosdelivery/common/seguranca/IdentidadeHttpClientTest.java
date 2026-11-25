package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.FalhaIntegracaoException;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IdentidadeHttpClientTest {

    private static final String BASE = "http://identidade:8001";
    private static final String CAMINHO = "/api/auth/validar";

    private MockRestServiceServer servidor;
    private IdentidadeHttpClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new IdentidadeHttpClient("Teste", BASE, CAMINHO, builder.build());
    }

    @Test
    void tokenValidoRetornaUsuarioEEnviaOBearer() {
        servidor.expect(requestTo(BASE + CAMINHO))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andRespond(withSuccess("{\"sub\":\"57\",\"role\":\"LOJISTA\",\"email\":\"x\"}", MediaType.APPLICATION_JSON));

        UsuarioAutenticado usuario = cliente.validarToken("abc.def.ghi");

        assertEquals(new UsuarioAutenticado(57L, "LOJISTA"), usuario);
        servidor.verify();
    }

    @Test
    void aceitaSubNumericoEIdComoSinonimo() {
        servidor.expect(requestTo(BASE + CAMINHO))
                .andRespond(withSuccess("{\"id\":57,\"role\":\"USER\"}", MediaType.APPLICATION_JSON));

        assertEquals(new UsuarioAutenticado(57L, "USER"), cliente.validarToken("t"));
    }

    @Test
    void tokenRecusadoPelaIdentidadeVira401() {
        servidor.expect(requestTo(BASE + CAMINHO)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        NaoAutenticadoException e = assertThrows(NaoAutenticadoException.class, () -> cliente.validarToken("t"));
        assertEquals("Token inválido ou expirado.", e.getMessage());
    }

    @Test
    void erro5xxDaIdentidadeVira503() {
        servidor.expect(requestTo(BASE + CAMINHO)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ServicoIndisponivelException e = assertThrows(ServicoIndisponivelException.class, () -> cliente.validarToken("t"));
        assertEquals("Serviço de identidade indisponível", e.getMessage());
    }

    @Test
    void identidadeForaDoArVira503() {
        servidor.expect(requestTo(BASE + CAMINHO)).andRespond(withException(new IOException("Connection refused")));

        ServicoIndisponivelException e = assertThrows(ServicoIndisponivelException.class, () -> cliente.validarToken("t"));
        assertEquals("Serviço de identidade indisponível", e.getMessage());
    }

    @Test
    void timeoutDaIdentidadeVira503() {
        servidor.expect(requestTo(BASE + CAMINHO)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(ServicoIndisponivelException.class, () -> cliente.validarToken("t"));
    }

    /** Sem mock do RestClient: porta aberta que nunca responde, para provar que o timeout está configurado. */
    @Test
    void timeoutRealConfiguradoNoClientNaoTravaARequisicao() throws IOException {
        try (ServerSocket mudo = new ServerSocket(0)) {
            IdentidadeHttpClient real = new IdentidadeHttpClient("Teste", "http://localhost:" + mudo.getLocalPort(),
                    CAMINHO, Duration.ofMillis(300));
            long inicio = System.currentTimeMillis();

            assertThrows(ServicoIndisponivelException.class, () -> real.validarToken("t"));
            assertEquals(true, System.currentTimeMillis() - inicio < 5_000);
        }
    }

    @Test
    void respostaSemSubVira502() {
        servidor.expect(requestTo(BASE + CAMINHO))
                .andRespond(withSuccess("{\"role\":\"LOJISTA\"}", MediaType.APPLICATION_JSON));

        assertThrows(FalhaIntegracaoException.class, () -> cliente.validarToken("t"));
    }

    @Test
    void outro4xxDaIdentidadeVira502() {
        servidor.expect(requestTo(BASE + CAMINHO)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(FalhaIntegracaoException.class, () -> cliente.validarToken("t"));
    }
}
