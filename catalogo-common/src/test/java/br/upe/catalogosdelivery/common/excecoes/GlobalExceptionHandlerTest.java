package br.upe.catalogosdelivery.common.excecoes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    static class ControllerDeTeste {
        @GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new RecursoNaoEncontradoException("Restaurante não encontrado");
        }

        @GetMapping("/indisponivel")
        void indisponivel() {
            throw new ServicoIndisponivelException("Cardápio fora do ar", null);
        }

        @GetMapping("/falha-integracao")
        void falhaIntegracao() {
            throw new FalhaIntegracaoException("Resposta inválida", null);
        }

        @GetMapping("/requisicao-invalida")
        void requisicaoInvalida() {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ownerUserId");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void recursoNaoEncontradoRetorna404NoFormatoPadrao() throws Exception {
        mockMvc.perform(get("/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Restaurante não encontrado"));
    }

    @Test
    void servicoIndisponivelRetorna503() throws Exception {
        mockMvc.perform(get("/indisponivel"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value(true));
    }

    @Test
    void falhaDeIntegracaoRetorna502() throws Exception {
        mockMvc.perform(get("/falha-integracao"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.erro").value(true));
    }

    @Test
    void responseStatusExceptionUsaOStatusEAMensagemNoFormatoPadrao() throws Exception {
        mockMvc.perform(get("/requisicao-invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Informe ownerUserId"));
    }
}
