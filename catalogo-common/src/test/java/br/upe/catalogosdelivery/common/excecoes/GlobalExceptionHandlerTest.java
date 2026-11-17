package br.upe.catalogosdelivery.common.excecoes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    record Entrada(@NotBlank String nome, int quantidade) {
    }

    @RestController
    static class ControllerDeTeste {
        @PostMapping("/entrada")
        void entrada(@Valid @RequestBody Entrada entrada) {
        }

        @GetMapping("/itens/{id}")
        void item(@PathVariable Long id) {
        }

        @GetMapping("/banco")
        void banco() {
            throw new DataIntegrityViolationException("value too long for type character varying(255)");
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalhe interno que não pode vazar");
        }

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

    @Test
    void jsonMalformadoRetorna400NoFormatoPadrao() throws Exception {
        mockMvc.perform(post("/entrada").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value(
                        "Corpo da requisição inválido: verifique o JSON, os campos obrigatórios e os valores aceitos."));
    }

    @Test
    void falhaDeValidacaoListaOsCampos() throws Exception {
        mockMvc.perform(post("/entrada").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\",\"quantidade\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.startsWith("nome: ")));
    }

    @Test
    void tipoErradoNaUrlRetorna400() throws Exception {
        mockMvc.perform(get("/itens/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor inválido para 'id'."));
    }

    @Test
    void metodoNaoSuportadoRetorna405() throws Exception {
        mockMvc.perform(post("/itens/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Método POST não suportado nesta rota."));
    }

    @Test
    void contentTypeNaoSuportadoRetorna415() throws Exception {
        mockMvc.perform(post("/entrada").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.erro").value(true));
    }

    @Test
    void dadosRecusadosPeloBancoRetornam400() throws Exception {
        mockMvc.perform(get("/banco"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(true));
    }

    @Test
    void erroInesperadoRetorna500SemExporDetalhes() throws Exception {
        mockMvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Erro interno no servidor."));
    }
}
