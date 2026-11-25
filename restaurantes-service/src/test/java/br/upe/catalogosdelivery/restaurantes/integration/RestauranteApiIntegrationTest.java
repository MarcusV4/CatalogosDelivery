package br.upe.catalogosdelivery.restaurantes.integration;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import br.upe.catalogosdelivery.common.seguranca.IdentidadeClient;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@AutoConfigureMockMvc
@Testcontainers
class RestauranteApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    /** Cadastro mínimo que passa no Bean Validation (status não é enviado: nasce ABERTO). */
    private static final String PAYLOAD_VALIDO = """
            {"restauranteNome":"Teste","restauranteDesc":"Descrição","logradouro":"Rua A","numero":"1",
             "bairro":"Centro","cidade":"Recife","estado":"PE","cep":"50000-000",
             "restauranteTelefone":"81999990000","taxaEntrega":0,"tempoEstimadoMin":20,"tempoEstimadoMax":40,
             "categoriaRestaurante":"PIZZARIA"}
            """;

    /** Tokens aceitos pelo mock da Identidade: o dono (42), outro lojista (99) e um cliente (7). */
    private static final String DONO = "Bearer token-dono";
    private static final String OUTRO_LOJISTA = "Bearer token-outro";
    private static final String CLIENTE = "Bearer token-cliente";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IdentidadeClient identidade;

    @BeforeEach
    void configurarIdentidade() {
        when(identidade.validarToken("token-dono")).thenReturn(new UsuarioAutenticado(42L, "LOJISTA"));
        when(identidade.validarToken("token-outro")).thenReturn(new UsuarioAutenticado(99L, "LOJISTA"));
        when(identidade.validarToken("token-cliente")).thenReturn(new UsuarioAutenticado(7L, "CLIENTE"));
        when(identidade.validarToken("token-forjado")).thenThrow(new NaoAutenticadoException("Token inválido ou expirado."));
    }

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    @Transactional
    void executaFluxoDeCadastroAtualizacaoStatusHorariosERemovacaoLogica() throws Exception {
        String criado = mockMvc.perform(post("/api/restaurantes").header("Authorization", DONO)
                        .contentType("application/json")
                        .content("""
                                {"restauranteNome":"Açaí Recife","restauranteDesc":"Teste",
                                 "logradouro":"Rua A","numero":"10","complemento":"Sala 1",
                                 "bairro":"Centro","cidade":"Recife","estado":"PE","cep":"50000-000",
                                 "restauranteTelefone":"81999990000","taxaEntrega":550,
                                 "tempoEstimadoMin":20,"tempoEstimadoMax":35,"urlImagem":"https://example.test/img",
                                 "ownerUserId":7,"categoriaRestaurante":"ACAI"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusRestaurante").value("ABERTO"))
                .andExpect(jsonPath("$.restauranteNome").value("Açaí Recife"))
                .andExpect(jsonPath("$.logradouro").value("Rua A"))
                .andExpect(jsonPath("$.restauranteTelefone").value("81999990000"))
                .andExpect(jsonPath("$.taxaEntrega").value(550))
                .andExpect(jsonPath("$.tempoEstimadoMax").value(35))
                // ownerUserId enviado no corpo (7) é ignorado: o dono é o sub do token (42).
                .andExpect(jsonPath("$.ownerUserId").value(42))
                .andExpect(jsonPath("$.categoriaRestaurante").value("ACAI"))
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(criado).get("restauranteId").asLong();

        mockMvc.perform(get("/api/restaurantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertoAgora").value(false));

        mockMvc.perform(put("/api/restaurantes/{id}", id).header("Authorization", DONO)
                        .contentType("application/json")
                        .content("""
                                {"restauranteNome":"Açaí Atualizado","restauranteDesc":"Descrição nova",
                                 "logradouro":"Rua B","numero":"20","complemento":"Loja",
                                 "bairro":"Boa Vista","cidade":"Recife","estado":"PE","cep":"50000-001",
                                 "restauranteTelefone":"81988880000","taxaEntrega":700,
                                 "tempoEstimadoMin":25,"tempoEstimadoMax":40,"urlImagem":"https://example.test/novo",
                                 "categoriaRestaurante":"COMIDA_SAUDAVEL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restauranteNome").value("Açaí Atualizado"))
                .andExpect(jsonPath("$.cidade").value("Recife"))
                .andExpect(jsonPath("$.taxaEntrega").value(700));

        mockMvc.perform(patch("/api/restaurantes/{id}/status", id).header("Authorization", DONO)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusRestaurante").value("FECHADO"))
                .andExpect(jsonPath("$.abertoAgora").value(false));

        String diaAtual = diaSemana(LocalDate.now(ZoneId.of("America/Recife")).getDayOfWeek());
        mockMvc.perform(put("/api/restaurantes/{id}/horarios", id).header("Authorization", DONO)
                        .contentType("application/json")
                        .content("""
                                {"horarios":[
                                  {"diaSemana":"%s","horaAbertura":"00:00:00","horaFechamento":"23:59:59"}
                                ]}
                                """.formatted(diaAtual)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diaSemana").value(diaAtual));

        mockMvc.perform(get("/api/restaurantes/{id}/horarios", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].horaAbertura").value("00:00:00"));

        mockMvc.perform(patch("/api/restaurantes/{id}/status", id).header("Authorization", DONO)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"ABERTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertoAgora").value(true));

        mockMvc.perform(delete("/api/restaurantes/{id}", id).header("Authorization", DONO))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/restaurantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusRestaurante").value("INDISPONIVEL"))
                .andExpect(jsonPath("$.abertoAgora").value(false));

        mockMvc.perform(get("/api/restaurantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.restauranteId == " + id + ")]").isEmpty());
    }

    @Test
    @Transactional
    void listagemCalculaAbertoAgoraECadastroSemNomeRetorna400() throws Exception {
        long id = criarRestaurante();
        mockMvc.perform(get("/api/restaurantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.restauranteId == " + id + ")].abertoAgora").value(false));

        mockMvc.perform(post("/api/restaurantes").header("Authorization", DONO)
                        .contentType("application/json")
                        .content(PAYLOAD_VALIDO.replace("\"restauranteNome\":\"Teste\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("restauranteNome: O nome do restaurante não pode ser vazio."));
    }

    @Test
    @Transactional
    void cadastroComCamposInvalidosRetorna400ENaoSalva() throws Exception {
        String invalido = PAYLOAD_VALIDO
                .replace("\"cep\":\"50000-000\"", "\"cep\":\"123\"")
                .replace("\"taxaEntrega\":0", "\"taxaEntrega\":-1");
        mockMvc.perform(post("/api/restaurantes").header("Authorization", DONO).contentType("application/json").content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("cep: CEP inválido"),
                        org.hamcrest.Matchers.containsString("taxaEntrega: A taxa de entrega não pode ser negativa."))));
        mockMvc.perform(get("/api/restaurantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cep == '123')]").isEmpty());
    }

    @Test
    @Transactional
    void horariosComCampoNuloRetorna400() throws Exception {
        long id = criarRestaurante();
        mockMvc.perform(put("/api/restaurantes/{id}/horarios", id).header("Authorization", DONO)
                        .contentType("application/json")
                        .content("""
                                {"horarios":[{"diaSemana":"SEGUNDA","horaAbertura":"08:00:00"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("horarios[0].horaFechamento: A hora de fechamento deve ser informada."));
    }

    @Test
    @Transactional
    void rejeitaAlteracoesDeOutroOwnerENaoApagaRestaurante() throws Exception {
        long id = criarRestaurante();
        mockMvc.perform(patch("/api/restaurantes/{id}/status", id).header("Authorization", OUTRO_LOJISTA)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Usuário não é proprietário do restaurante."));
        mockMvc.perform(delete("/api/restaurantes/{id}", id).header("Authorization", OUTRO_LOJISTA))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/restaurantes/{id}/horarios", id).header("Authorization", OUTRO_LOJISTA)
                        .contentType("application/json").content("{\"horarios\":[]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/restaurantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusRestaurante").value("ABERTO"));
    }

    @Test
    @Transactional
    void semTokenOuComTokenInvalidoRetorna401ENaoAltera() throws Exception {
        long id = criarRestaurante();
        mockMvc.perform(delete("/api/restaurantes/{id}", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(true));
        mockMvc.perform(delete("/api/restaurantes/{id}", id).header("Authorization", "Bearer token-forjado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado."));
        mockMvc.perform(post("/api/restaurantes").contentType("application/json").content(PAYLOAD_VALIDO))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/restaurantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusRestaurante").value("ABERTO"));
    }

    @Test
    @Transactional
    void identidadeForaDoArRetorna503EAsLeiturasContinuam() throws Exception {
        long id = criarRestaurante();
        when(identidade.validarToken("token-dono"))
                .thenThrow(new ServicoIndisponivelException("Serviço de identidade indisponível", null));

        mockMvc.perform(patch("/api/restaurantes/{id}/status", id).header("Authorization", DONO)
                        .contentType("application/json").content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"erro\":true,\"mensagem\":\"Serviço de identidade indisponível\"}"));
        mockMvc.perform(get("/api/restaurantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusRestaurante").value("ABERTO"));
    }

    @Test
    @Transactional
    void somentePerfilDeLojaCadastraRestaurante() throws Exception {
        mockMvc.perform(post("/api/restaurantes").header("Authorization", CLIENTE)
                        .contentType("application/json").content(PAYLOAD_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Apenas usuários com perfil LOJISTA podem cadastrar restaurantes."));
        mockMvc.perform(get("/api/restaurantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ownerUserId == 7)]").isEmpty());
    }

    @Test
    void swaggerListaRotasDeGerenciamentoDeRestaurantes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}/horarios'].get").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}/horarios'].put").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}'].delete").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/restaurantes'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}'].delete.responses['503']").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}'].delete.parameters[?(@.name == 'ownerUserId')]").isEmpty());
    }

    private long criarRestaurante() throws Exception {
        String response = mockMvc.perform(post("/api/restaurantes").header("Authorization", DONO)
                        .contentType("application/json")
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("restauranteId").asLong();
    }

    private String diaSemana(DayOfWeek dia) {
        return switch (dia) {
            case SUNDAY -> "DOMINGO";
            case MONDAY -> "SEGUNDA";
            case TUESDAY -> "TERCA";
            case WEDNESDAY -> "QUARTA";
            case THURSDAY -> "QUINTA";
            case FRIDAY -> "SEXTA";
            case SATURDAY -> "SABADO";
        };
    }
}
