package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import br.upe.catalogosdelivery.common.seguranca.ValidadorDeTokens;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.FuncionamentoRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.HorarioFuncionamentoDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.SubstituirHorariosDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.exceptions.RestauranteNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestauranteController.class)
class RestauranteControllerTest {

    /** Sem cabeçalho Authorization: testa o 401 das escritas. */
    @Autowired
    private MockMvc semToken;

    @Autowired
    private WebApplicationContext contexto;

    /** Envia o token de uma conta LOJA em toda requisição (as leituras o ignoram). */
    private MockMvc mockMvc;

    @MockitoBean
    private ValidadorDeTokens validadorDeTokens;

    private static final String CONTA_LOJA = "6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01";

    @BeforeEach
    void autenticarComoLoja() {
        when(validadorDeTokens.validar("token-loja")).thenReturn(new UsuarioAutenticado(CONTA_LOJA, "LOJA"));
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto)
                .defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer token-loja"))
                .build();
    }

    @MockitoBean
    private RestauranteService restauranteService;

    @MockitoBean
    private RestauranteMapper restauranteMapper;

    private static final long ID_INEXISTENTE = 99999L;

    /** Requisições válidas para um id inexistente, uma por endpoint do critério de aceitação da #63. */
    private static Stream<Arguments> requisicoesComIdInexistente() {
        String url = "/api/restaurantes/" + ID_INEXISTENTE;
        String dadosValidos = """
                {"restauranteNome":"Pizzaria","restauranteDesc":"Massas","logradouro":"Rua A","numero":"10",
                 "bairro":"Centro","cidade":"Recife","estado":"PE","cep":"50000-000",
                 "restauranteTelefone":"81999990000","taxaEntrega":500,"tempoEstimadoMin":20,
                 "tempoEstimadoMax":40,"categoriaRestaurante":"PIZZARIA"}
                """;
        return Stream.of(
                Arguments.of("GET /{id}", get(url)),
                Arguments.of("PUT /{id}", put(url).contentType("application/json").content(dadosValidos)),
                Arguments.of("DELETE /{id}", delete(url)),
                Arguments.of("PATCH /{id}/status", patch(url + "/status").contentType("application/json")
                        .content("{\"statusRestaurante\":\"FECHADO\"}")),
                Arguments.of("GET /{id}/horarios", get(url + "/horarios")),
                Arguments.of("PUT /{id}/horarios", put(url + "/horarios").contentType("application/json")
                        .content("{\"horarios\":[]}")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requisicoesComIdInexistente")
    void restauranteInexistenteRetorna404ComMensagemPadrao(String endpoint, MockHttpServletRequestBuilder requisicao)
            throws Exception {
        RestauranteNaoEncontradoException naoEncontrado = new RestauranteNaoEncontradoException(ID_INEXISTENTE);
        when(restauranteService.buscarRestaurante(ID_INEXISTENTE)).thenThrow(naoEncontrado);
        when(restauranteService.atualizarRestaurante(eq(ID_INEXISTENTE), any(AtualizarRestauranteDTO.class)))
                .thenThrow(naoEncontrado);
        when(restauranteService.atualizarStatus(eq(ID_INEXISTENTE), any(AtualizarStatusRestauranteDTO.class)))
                .thenThrow(naoEncontrado);
        when(restauranteService.listarHorarios(ID_INEXISTENTE)).thenThrow(naoEncontrado);
        when(restauranteService.substituirHorarios(eq(ID_INEXISTENTE), any(SubstituirHorariosDTO.class)))
                .thenThrow(naoEncontrado);
        doThrow(naoEncontrado).when(restauranteService).deletarRestaurante(ID_INEXISTENTE);

        mockMvc.perform(requisicao)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Restaurante 99999 não encontrado."));
    }

    /** Requisições inválidas (corpo ou parâmetro) para um id inexistente: o 404 vem antes do 400. */
    private static Stream<Arguments> requisicoesInvalidasComIdInexistente() {
        String url = "/api/restaurantes/" + ID_INEXISTENTE;
        return Stream.of(
                Arguments.of("PUT /{id} com corpo inválido", put(url).contentType("application/json")
                        .content("{\"restauranteNome\":\" \",\"estado\":\"Pernambuco\"}")),
                Arguments.of("PATCH /{id}/status sem status", patch(url + "/status")
                        .contentType("application/json").content("{}")),
                Arguments.of("PUT /{id}/horarios sem horarios", put(url + "/horarios")
                        .contentType("application/json").content("{}")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requisicoesInvalidasComIdInexistente")
    void restauranteInexistenteRetorna404AntesDeValidarARequisicao(String endpoint,
            MockHttpServletRequestBuilder requisicao) throws Exception {
        doThrow(new RestauranteNaoEncontradoException(ID_INEXISTENTE))
                .when(restauranteService).verificarExistencia(ID_INEXISTENTE);

        mockMvc.perform(requisicao)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Restaurante 99999 não encontrado."));
        verify(restauranteService, never()).atualizarRestaurante(any(), any());
        verify(restauranteService, never()).atualizarStatus(any(), any());
        verify(restauranteService, never()).substituirHorarios(any(), any());
        verify(restauranteService, never()).deletarRestaurante(any());
    }

    @Test
    void idNaoNumericoContinuaRetornando400SemConsultarORestaurante() throws Exception {
        mockMvc.perform(get("/api/restaurantes/abc")).andExpect(status().isBadRequest());
        verify(restauranteService, never()).verificarExistencia(any());
    }

    @Test
    void listaRestaurantesComoJson() throws Exception {
        when(restauranteService.listarRestaurante()).thenReturn(List.of(restauranteDTO()));

        mockMvc.perform(get("/api/restaurantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restauranteId").value(10))
                .andExpect(jsonPath("$[0].restauranteNome").value("Pizzaria Recife"))
                .andExpect(jsonPath("$[0].taxaEntrega").value(550));
    }

    @Test
    void buscaRestaurantePorId() throws Exception {
        when(restauranteService.buscarRestaurante(10L)).thenReturn(restauranteDTO());

        mockMvc.perform(get("/api/restaurantes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restauranteId").value(10))
                .andExpect(jsonPath("$.categoriaRestaurante").value("PIZZARIA"))
                .andExpect(jsonPath("$.pedidoMinimo").value(2000));
    }

    @Test
    void cadastraRestauranteERetorna201() throws Exception {
        Restaurante salvo = new Restaurante();
        salvo.setRestauranteId(10L);
        salvo.setRestauranteNome("Pizzaria Recife");
        when(restauranteService.cadastrarRestaurante(any(RestauranteDTO.class))).thenReturn(salvo);
        when(restauranteMapper.toResponseDTO(salvo)).thenReturn(restauranteDTO());

        mockMvc.perform(post("/api/restaurantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restauranteNome":"Pizzaria Recife","restauranteDesc":"Pizza artesanal",
                                 "logradouro":"Rua A","numero":"10","bairro":"Centro","cidade":"Recife",
                                 "estado":"PE","cep":"50000-000","restauranteTelefone":"81999999999",
                                 "taxaEntrega":550,"tempoEstimadoMin":30,"tempoEstimadoMax":45,
                                 "categoriaRestaurante":"PIZZARIA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restauranteNome").value("Pizzaria Recife"));

        verify(restauranteService).cadastrarRestaurante(any(RestauranteDTO.class));
    }

    @Test
    void excluiRestauranteLogicamenteERetorna204() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/10"))
                .andExpect(status().isNoContent());

        verify(restauranteService).deletarRestaurante(10L);
    }

    @Test
    void escritaSemTokenRetorna401SemChamarService() throws Exception {
        semToken.perform(delete("/api/restaurantes/5"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(true));
        semToken.perform(post("/api/restaurantes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verify(restauranteService, never()).deletarRestaurante(any());
        verify(restauranteService, never()).cadastrarRestaurante(any());
    }

    @Test
    void escritaSemTokenEmRestauranteInexistenteResponde401AntesDo404() throws Exception {
        semToken.perform(put("/api/restaurantes/" + ID_INEXISTENTE + "/horarios")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"horarios\":[]}"))
                .andExpect(status().isUnauthorized());

        verify(restauranteService, never()).verificarExistencia(any());
    }

    @Test
    void leituraContinuaPublicaSemToken() throws Exception {
        when(restauranteService.listarRestaurante()).thenReturn(List.of(restauranteDTO()));

        semToken.perform(get("/api/restaurantes")).andExpect(status().isOk());
    }

    @Test
    void statusPodeSerAlteradoPeloEndpointPatch() throws Exception {
        when(restauranteService.atualizarStatus(eq(5L), any(AtualizarStatusRestauranteDTO.class)))
                .thenReturn(resposta());

        mockMvc.perform(patch("/api/restaurantes/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isOk());

        verify(restauranteService).atualizarStatus(eq(5L), any(AtualizarStatusRestauranteDTO.class));
    }

    @Test
    void cadastroInvalidoRetorna400SemChamarService() throws Exception {
        mockMvc.perform(post("/api/restaurantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restauranteNome\":\" \",\"estado\":\"Pernambuco\"}"))
                .andExpect(status().isBadRequest());

        verify(restauranteService, never()).cadastrarRestaurante(any());
    }

    @Test
    void cadastroComPedidoMinimoNegativoRetorna400SemChamarService() throws Exception {
        mockMvc.perform(post("/api/restaurantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restauranteNome":"Pizzaria Recife","restauranteDesc":"Pizza artesanal",
                                 "logradouro":"Rua A","numero":"10","bairro":"Centro","cidade":"Recife",
                                 "estado":"PE","cep":"50000-000","restauranteTelefone":"81999999999",
                                 "taxaEntrega":550,"pedidoMinimo":-100,"tempoEstimadoMin":30,"tempoEstimadoMax":45,
                                 "categoriaRestaurante":"PIZZARIA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString(
                        "O pedido mínimo não pode ser negativo.")));

        verify(restauranteService, never()).cadastrarRestaurante(any());
    }

    @Test
    void funcionamentoTrazOPedidoMinimo() throws Exception {
        when(restauranteService.buscarFuncionamento(5L)).thenReturn(new FuncionamentoRestauranteDTO(
                StatusRestaurante.ABERTO, true, DiaSemana.SEGUNDA, List.of(), 2000L));

        mockMvc.perform(get("/api/restaurantes/5/funcionamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoMinimo").value(2000));
    }

    @Test
    void statusSemValorRetorna400SemChamarService() throws Exception {
        mockMvc.perform(patch("/api/restaurantes/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(restauranteService, never()).atualizarStatus(any(), any());
    }

    @Test
    void endpointRetornaHorariosDaSemana() throws Exception {
        when(restauranteService.listarHorarios(5L)).thenReturn(List.of(
                new HorarioFuncionamentoDTO(DiaSemana.SEGUNDA, LocalTime.of(9, 0), LocalTime.of(18, 0))));

        mockMvc.perform(get("/api/restaurantes/5/horarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diaSemana").value("SEGUNDA"));

        verify(restauranteService).listarHorarios(5L);
    }

    private static RestauranteResponseDTO restauranteDTO() {
        return new RestauranteResponseDTO(10L, "Pizzaria Recife", "Pizza artesanal", "Rua A", "10", null,
                "Centro", "Recife", "PE", "50000-000", "81999999999", null, CONTA_LOJA,
                StatusRestaurante.ABERTO, CategoriaRestaurante.PIZZARIA, 550L, 2000L, 30, 45, true);
    }

    private static RestauranteResponseDTO resposta() {
        return new RestauranteResponseDTO(5L, "Pizzaria", null, null, null, null, null, null, null, null,
                null, null, CONTA_LOJA, StatusRestaurante.FECHADO, CategoriaRestaurante.PIZZARIA, 0L,
                null, null, null, false);
    }
}
