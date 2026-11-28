package br.upe.catalogosdelivery.restaurantes.integration;

import org.junit.jupiter.api.Test;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Modo mock da Identidade: aceita JWT sem assinatura, para focar nas regras de dono e papel.
 * A validação real (RS256/JWKS, iss, aud, exp e RBAC) é testada em catalogo-common.
 */
@SpringBootTest(properties = {"spring.jpa.show-sql=false", "catalogo.identidade.modo=mock"})
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

    /** Contas da Identidade (sub = UUID da conta). */
    private static final String DONO = "6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01";
    private static final String TOKEN_DONO = tokenDev(DONO, "LOJA");
    private static final String TOKEN_OUTRA_LOJA = tokenDev("0a9b8c7d-6e5f-4a3b-9c2d-1e0f9a8b7c6d", "LOJA");
    private static final String TOKEN_CLIENTE = tokenDev("11111111-2222-4333-8444-555555555555", "CLIENTE");

    /** Sem cabeçalho Authorization. */
    @Autowired private MockMvc semToken;
    @Autowired private WebApplicationContext contexto;
    /** Envia o token do DONO em toda requisição. */
    private MockMvc mockMvc;

    @BeforeEach
    void autenticarComoDono() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_DONO))
                .build();
    }

    /** JWT sem assinatura aceito pelo modo mock (alg none). */
    private static String tokenDev(String conta, String papel) {
        java.util.Base64.Encoder b64 = java.util.Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString("{\"alg\":\"none\"}".getBytes()) + "."
                + b64.encodeToString(("{\"sub\":\"" + conta + "\",\"role\":\"" + papel + "\"}").getBytes()) + ".";
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
        String criado = mockMvc.perform(post("/api/restaurantes")
                        .contentType("application/json")
                        .content("""
                                {"restauranteNome":"Açaí Recife","restauranteDesc":"Teste",
                                 "logradouro":"Rua A","numero":"10","complemento":"Sala 1",
                                 "bairro":"Centro","cidade":"Recife","estado":"PE","cep":"50000-000",
                                 "restauranteTelefone":"81999990000","taxaEntrega":550,"pedidoMinimo":2000,
                                 "tempoEstimadoMin":20,"tempoEstimadoMax":35,"urlImagem":"https://example.test/img",
                                 "categoriaRestaurante":"ACAI"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusRestaurante").value("ABERTO"))
                .andExpect(jsonPath("$.restauranteNome").value("Açaí Recife"))
                .andExpect(jsonPath("$.logradouro").value("Rua A"))
                .andExpect(jsonPath("$.restauranteTelefone").value("81999990000"))
                .andExpect(jsonPath("$.taxaEntrega").value(550))
                .andExpect(jsonPath("$.pedidoMinimo").value(2000))
                .andExpect(jsonPath("$.tempoEstimadoMax").value(35))
                .andExpect(jsonPath("$.ownerUserId").value(DONO))
                .andExpect(jsonPath("$.categoriaRestaurante").value("ACAI"))
                .andReturn().getResponse().getContentAsString();
        long id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(criado).get("restauranteId").asLong();

        mockMvc.perform(get("/api/restaurantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertoAgora").value(false))
                .andExpect(jsonPath("$.pedidoMinimo").value(2000));

        mockMvc.perform(get("/api/restaurantes/{id}/funcionamento", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoMinimo").value(2000));

        mockMvc.perform(put("/api/restaurantes/{id}", id)
                        .contentType("application/json")
                        .content("""
                                {"restauranteNome":"Açaí Atualizado","restauranteDesc":"Descrição nova",
                                 "logradouro":"Rua B","numero":"20","complemento":"Loja",
                                 "bairro":"Boa Vista","cidade":"Recife","estado":"PE","cep":"50000-001",
                                 "restauranteTelefone":"81988880000","taxaEntrega":700,"pedidoMinimo":null,
                                 "tempoEstimadoMin":25,"tempoEstimadoMax":40,"urlImagem":"https://example.test/novo",
                                 "categoriaRestaurante":"COMIDA_SAUDAVEL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restauranteNome").value("Açaí Atualizado"))
                .andExpect(jsonPath("$.cidade").value("Recife"))
                .andExpect(jsonPath("$.taxaEntrega").value(700))
                .andExpect(jsonPath("$.pedidoMinimo").value(nullValue()));

        mockMvc.perform(get("/api/restaurantes/{id}/funcionamento", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoMinimo").value(nullValue()));

        mockMvc.perform(patch("/api/restaurantes/{id}/status", id)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusRestaurante").value("FECHADO"))
                .andExpect(jsonPath("$.abertoAgora").value(false));

        String diaAtual = diaSemana(LocalDate.now(ZoneId.of("America/Recife")).getDayOfWeek());
        mockMvc.perform(put("/api/restaurantes/{id}/horarios", id)
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

        mockMvc.perform(patch("/api/restaurantes/{id}/status", id)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"ABERTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertoAgora").value(true));

        mockMvc.perform(delete("/api/restaurantes/{id}", id))
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

        mockMvc.perform(post("/api/restaurantes")
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
        mockMvc.perform(post("/api/restaurantes").contentType("application/json").content(invalido))
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
        mockMvc.perform(put("/api/restaurantes/{id}/horarios", id)
                        .contentType("application/json")
                        .content("""
                                {"horarios":[{"diaSemana":"SEGUNDA","horaAbertura":"08:00:00"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("horarios[0].horaFechamento: A hora de fechamento deve ser informada."));
    }

    /** O dia é gravado como texto: a resposta segue a ordem da semana, não a alfabética nem a de envio. */
    @Test
    @Transactional
    void putEGetDeHorariosDevolvemOsDiasNaOrdemDaSemana() throws Exception {
        long id = criarRestaurante();
        String[] ordemDaSemana = {"SEGUNDA", "QUARTA", "SEXTA", "SABADO"};
        mockMvc.perform(put("/api/restaurantes/{id}/horarios", id)
                        .contentType("application/json")
                        .content("""
                                {"horarios":[
                                  {"diaSemana":"SEXTA","horaAbertura":"18:00:00","horaFechamento":"23:00:00"},
                                  {"diaSemana":"SEGUNDA","horaAbertura":"09:00:00","horaFechamento":"17:00:00"},
                                  {"diaSemana":"QUARTA","horaAbertura":"10:00:00","horaFechamento":"16:00:00"},
                                  {"diaSemana":"SABADO","horaAbertura":"11:00:00","horaFechamento":"15:00:00"}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].diaSemana").value(org.hamcrest.Matchers.contains(ordemDaSemana)))
                .andExpect(jsonPath("$[0].horaAbertura").value("09:00:00"));

        mockMvc.perform(get("/api/restaurantes/{id}/horarios", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].diaSemana").value(org.hamcrest.Matchers.contains(ordemDaSemana)))
                .andExpect(jsonPath("$[2].horaFechamento").value("23:00:00"));
    }

    @Test
    @Transactional
    void rejeitaAlteracoesDeOutroOwnerENaoApagaRestaurante() throws Exception {
        long id = criarRestaurante();
        mockMvc.perform(patch("/api/restaurantes/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_OUTRA_LOJA)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/restaurantes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_OUTRA_LOJA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/restaurantes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusRestaurante").value("ABERTO"));
    }

    @Test
    void restauranteInexistenteRetorna404MesmoComRequisicaoInvalida() throws Exception {
        String mensagem = "Restaurante 99999 não encontrado.";
        mockMvc.perform(put("/api/restaurantes/99999").contentType("application/json")
                        .content("{\"restauranteNome\":\" \"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(mensagem));
        mockMvc.perform(delete("/api/restaurantes/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(mensagem));
        mockMvc.perform(patch("/api/restaurantes/99999/status").contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(mensagem));
        mockMvc.perform(put("/api/restaurantes/99999/horarios").contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    @Test
    @Transactional
    void restauranteExistenteComRequisicaoInvalidaContinuaRetornando400() throws Exception {
        long id = criarRestaurante();
        mockMvc.perform(put("/api/restaurantes/{id}", id).contentType("application/json")
                        .content("{\"restauranteNome\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/restaurantes/{id}/status", id).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idNaoNumericoContinuaRetornando400() throws Exception {
        mockMvc.perform(get("/api/restaurantes/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void preFlightDeCorsNaoConsultaORestaurante() throws Exception {
        mockMvc.perform(options("/api/restaurantes/99999")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void funcionamentoMostraAbertoAgoraComOsHorariosDeHojeEFechadoQuandoOStatusEhFechado() throws Exception {
        long id = criarRestaurante();
        String hoje = diaSemana(LocalDate.now(ZoneId.of("America/Recife")).getDayOfWeek());
        String outroDia = hoje.equals("SEGUNDA") ? "TERCA" : "SEGUNDA";
        mockMvc.perform(put("/api/restaurantes/{id}/horarios", id)
                        .contentType("application/json")
                        .content("""
                                {"horarios":[
                                  {"diaSemana":"%s","horaAbertura":"00:00:00","horaFechamento":"23:59:59"},
                                  {"diaSemana":"%s","horaAbertura":"10:00:00","horaFechamento":"22:00:00"}
                                ]}
                                """.formatted(hoje, outroDia)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/restaurantes/{id}/funcionamento", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.abertoAgora").value(true))
                .andExpect(jsonPath("$.diaSemana").value(hoje))
                .andExpect(jsonPath("$.horarios.length()").value(1))
                .andExpect(jsonPath("$.horarios[0].diaSemana").value(hoje))
                .andExpect(jsonPath("$.horarios[0].horaAbertura").value("00:00:00"));

        mockMvc.perform(patch("/api/restaurantes/{id}/status", id)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/restaurantes/{id}/funcionamento", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADO"))
                .andExpect(jsonPath("$.abertoAgora").value(false))
                .andExpect(jsonPath("$.horarios.length()").value(1));
    }

    @Test
    @Transactional
    void funcionamentoSemHorarioCadastradoNaoEstaAberto() throws Exception {
        long id = criarRestaurante();

        mockMvc.perform(get("/api/restaurantes/{id}/funcionamento", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.abertoAgora").value(false))
                .andExpect(jsonPath("$.horarios").isEmpty());
    }

    @Test
    void funcionamentoDeRestauranteInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/restaurantes/99999/funcionamento"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Restaurante 99999 não encontrado."));
    }

    @Test
    void swaggerListaRotasDeGerenciamentoDeRestaurantes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}/horarios'].get").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}/horarios'].put").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}/funcionamento'].get").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes/{idRestaurante}'].delete").exists());
    }

    @Test
    void escritasSemTokenRespondem401() throws Exception {
        semToken.perform(post("/api/restaurantes").contentType("application/json").content(PAYLOAD_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(true));
        semToken.perform(delete("/api/restaurantes/99999")).andExpect(status().isUnauthorized());
        semToken.perform(post("/api/restaurantes").contentType("application/json").content(PAYLOAD_VALIDO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contaQueNaoEhDeLojaNaoCadastraRestaurante() throws Exception {
        mockMvc.perform(post("/api/restaurantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_CLIENTE)
                        .contentType("application/json")
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Somente contas de loja podem gerenciar restaurantes e cardápios."));
    }

    @Test
    @Transactional
    void donoVemDoTokenMesmoQueOCorpoTenteInformarOutro() throws Exception {
        mockMvc.perform(post("/api/restaurantes")
                        .contentType("application/json")
                        .content(PAYLOAD_VALIDO.replace("\"categoriaRestaurante\"",
                                "\"ownerUserId\":\"0a9b8c7d-6e5f-4a3b-9c2d-1e0f9a8b7c6d\",\"categoriaRestaurante\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerUserId").value(DONO));
    }

    @Test
    void swaggerMostraOCadeadoSoNasEscritas() throws Exception {
        semToken.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/restaurantes'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/restaurantes'].get.security").doesNotExist());
    }

    private long criarRestaurante() throws Exception {
        String response = mockMvc.perform(post("/api/restaurantes")
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
