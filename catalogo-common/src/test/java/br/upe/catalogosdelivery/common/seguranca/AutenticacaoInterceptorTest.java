package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.GlobalExceptionHandler;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Os 4 cenários da #31 na borda do serviço: 401 sem token, 401 token inválido, 503 Identidade fora e sucesso. */
class AutenticacaoInterceptorTest {

    private IdentidadeClient identidade;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        identidade = mock(IdentidadeClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste(new UsuarioAtual()))
                .addInterceptors(new AutenticacaoInterceptor(identidade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void escritaSemTokenRetorna401NoFormatoDaPlataforma() throws Exception {
        mvc.perform(post("/api/teste"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value(
                        "Token de autenticação ausente. Envie o header Authorization: Bearer <token>."));
        verifyNoInteractions(identidade);
    }

    @Test
    void headerSemBearerRetorna401() throws Exception {
        for (String valor : List.of("Basic dXNlcjpzZW5oYQ==", "Bearer ", "token-solto")) {
            mvc.perform(delete("/api/teste").header(HttpHeaders.AUTHORIZATION, valor))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensagem").value("Header Authorization inválido. Use o formato Bearer <token>."));
        }
        verifyNoInteractions(identidade);
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        when(identidade.validarToken("vencido")).thenThrow(new NaoAutenticadoException("Token inválido ou expirado."));

        mvc.perform(post("/api/teste").header(HttpHeaders.AUTHORIZATION, "Bearer vencido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado."));
    }

    @Test
    void identidadeForaDoArRetorna503SemDerrubarOServico() throws Exception {
        when(identidade.validarToken("t"))
                .thenThrow(new ServicoIndisponivelException("Serviço de identidade indisponível", null));

        mvc.perform(post("/api/teste").header(HttpHeaders.AUTHORIZATION, "Bearer t"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"erro\":true,\"mensagem\":\"Serviço de identidade indisponível\"}"));
        // As leituras continuam respondendo normalmente.
        mvc.perform(get("/api/teste")).andExpect(status().isOk());
    }

    @Test
    void tokenValidoDisponibilizaOUsuarioParaOController() throws Exception {
        when(identidade.validarToken("abc")).thenReturn(new UsuarioAutenticado(57L, "LOJISTA"));

        mvc.perform(post("/api/teste").header(HttpHeaders.AUTHORIZATION, "bearer abc"))
                .andExpect(status().isOk())
                .andExpect(content().string("57"));
    }

    @Test
    void leituraERotaPublicaNaoExigemToken() throws Exception {
        mvc.perform(get("/api/teste")).andExpect(status().isOk());
        mvc.perform(post("/api/teste/publica")).andExpect(status().isOk());
        mvc.perform(options("/api/teste")).andExpect(status().isOk());
        verifyNoInteractions(identidade);
    }

    @Test
    void usuarioAtualSemAutenticacaoLanca401() {
        assertEquals("Token de autenticação ausente. Envie o header Authorization: Bearer <token>.",
                assertThrows(NaoAutenticadoException.class, () -> new UsuarioAtual().obter()).getMessage());
    }

    @Test
    void roleIgnoraMaiusculasEPrefixoDoSpring() {
        assertTrue(new UsuarioAutenticado(1L, "ROLE_lojista").possuiAlgumaRole(List.of("LOJISTA")));
        assertTrue(new UsuarioAutenticado(1L, "ADMIN").possuiAlgumaRole(List.of("LOJISTA", "admin")));
        assertFalse(new UsuarioAutenticado(1L, "CLIENTE").possuiAlgumaRole(List.of("LOJISTA")));
        assertFalse(new UsuarioAutenticado(1L, null).possuiAlgumaRole(List.of("LOJISTA")));
    }

    @RestController
    @RequestMapping("/api/teste")
    static class ControllerDeTeste {
        private final UsuarioAtual usuarioAtual;

        ControllerDeTeste(UsuarioAtual usuarioAtual) {
            this.usuarioAtual = usuarioAtual;
        }

        @GetMapping
        String ler() {
            return "ok";
        }

        @PostMapping
        String escrever() {
            return String.valueOf(usuarioAtual.obter().id());
        }

        @DeleteMapping
        void excluir() {
        }

        @RotaPublica
        @PostMapping("/publica")
        String publica() {
            return "ok";
        }
    }
}
