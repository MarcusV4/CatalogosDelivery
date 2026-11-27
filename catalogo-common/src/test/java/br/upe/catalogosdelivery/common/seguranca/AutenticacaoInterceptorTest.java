package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.GlobalExceptionHandler;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacaoInterceptorTest {

    private static final UsuarioAtual USUARIO_ATUAL = new UsuarioAtual(new IdentidadeProperties(
            IdentidadeProperties.Modo.MOCK, "x", null, List.of(), null, false, Duration.ofSeconds(1), List.of("LOJA")));

    private final ValidadorDeTokens validador = mock(ValidadorDeTokens.class);

    @RestController
    @RequestMapping("/api/teste")
    static class ControllerDeTeste {

        @GetMapping
        String ler() {
            return "leitura publica";
        }

        @PostMapping
        String escrever() {
            return "escrito por " + USUARIO_ATUAL.obterLoja().contaId();
        }

        @RotaPublica
        @PostMapping("/publica")
        String escritaPublica() {
            return "rota publica";
        }
    }

    private MockMvc mockMvc(ObjectProvider<ValidadorDeTokens> provedor) {
        return MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addMappedInterceptors(new String[]{"/api/**"}, new AutenticacaoInterceptor(provedor))
                .build();
    }

    private MockMvc mockMvc() {
        StaticListableBeanFactory fabrica = new StaticListableBeanFactory();
        fabrica.addBean("validador", validador);
        return mockMvc(fabrica.getBeanProvider(ValidadorDeTokens.class));
    }

    @Test
    void leituraNaoExigeToken() throws Exception {
        mockMvc().perform(get("/api/teste")).andExpect(status().isOk());
        verify(validador, never()).validar(anyString());
    }

    @Test
    void escritaSemTokenResponde401NoFormatoPadrao() throws Exception {
        mockMvc().perform(post("/api/teste"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value(
                        "Entre com sua conta para continuar (cabeçalho Authorization: Bearer <token>)."));
    }

    @Test
    void escritaComTokenValidoChegaAoControllerComOUsuario() throws Exception {
        when(validador.validar("abc")).thenReturn(new UsuarioAutenticado(TokensDeTeste.CONTA, "LOJA"));

        mockMvc().perform(post("/api/teste").header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(content().string("escrito por " + TokensDeTeste.CONTA));
    }

    @Test
    void tokenRecusadoPeloValidadorResponde401() throws Exception {
        when(validador.validar("vencido")).thenThrow(new NaoAutenticadoException("Token expirado."));

        mockMvc().perform(post("/api/teste").header(HttpHeaders.AUTHORIZATION, "Bearer vencido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token expirado."));
    }

    @Test
    void contaQueNaoEhDeLojaResponde403() throws Exception {
        when(validador.validar("cliente")).thenReturn(new UsuarioAutenticado(TokensDeTeste.CONTA, "CLIENTE"));

        mockMvc().perform(post("/api/teste").header(HttpHeaders.AUTHORIZATION, "Bearer cliente"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(
                        "Somente contas de loja podem gerenciar restaurantes e cardápios."));
    }

    @Test
    void rotaPublicaPassaSemToken() throws Exception {
        mockMvc().perform(post("/api/teste/publica")).andExpect(status().isOk());
        verify(validador, never()).validar(anyString());
    }

    @Test
    void preFlightDeCorsPassaSemToken() {
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/api/teste");
        preflight.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
        preflight.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        StaticListableBeanFactory fabrica = new StaticListableBeanFactory();
        fabrica.addBean("validador", validador);

        assertTrue(new AutenticacaoInterceptor(fabrica.getBeanProvider(ValidadorDeTokens.class))
                .preHandle(preflight, new MockHttpServletResponse(), new Object()));
        verify(validador, never()).validar(anyString());
    }

    @Test
    void semValidadorConfiguradoResponde503() throws Exception {
        mockMvc(new StaticListableBeanFactory().getBeanProvider(ValidadorDeTokens.class))
                .perform(post("/api/teste").header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(status().isServiceUnavailable());
    }
}
