package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.common.excecoes.GlobalExceptionHandler;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.SubstituirHorariosDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.exceptions.RestauranteNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

class RestauranteControllerTest {

    private RestauranteService restauranteService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        restauranteService = mock(RestauranteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RestauranteController(restauranteService,
                        new RestauranteMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new RestauranteExistenteInterceptor(restauranteService))
                .build();
    }

    private static final long ID_INEXISTENTE = 99999L;

    /** Requisições válidas para um id inexistente, uma por endpoint do critério de aceitação da #63. */
    private static Stream<Arguments> requisicoesComIdInexistente() {
        String url = "/api/restaurantes/" + ID_INEXISTENTE;
        String dadosValidos = """
                {"restauranteNome":"Pizzaria","restauranteDesc":"Massas","logradouro":"Rua A","numero":"10",
                 "bairro":"Centro","cidade":"Recife","estado":"PE","cep":"50000-000",
                 "restauranteTelefone":"81999990000","taxaEntrega":500,"tempoEstimadoMin":20,
                 "tempoEstimadoMax":40,"ownerUserId":10,"categoriaRestaurante":"PIZZARIA"}
                """;
        return Stream.of(
                Arguments.of("GET /{id}", get(url)),
                Arguments.of("PUT /{id}", put(url).contentType("application/json").content(dadosValidos)),
                Arguments.of("DELETE /{id}", delete(url).param("ownerUserId", "10")),
                Arguments.of("PATCH /{id}/status", patch(url + "/status").contentType("application/json")
                        .content("{\"ownerUserId\":10,\"statusRestaurante\":\"FECHADO\"}")),
                Arguments.of("GET /{id}/horarios", get(url + "/horarios")),
                Arguments.of("PUT /{id}/horarios", put(url + "/horarios").contentType("application/json")
                        .content("{\"ownerUserId\":10,\"horarios\":[]}")));
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
        doThrow(naoEncontrado).when(restauranteService).deletarRestaurante(eq(ID_INEXISTENTE), any());

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
                Arguments.of("DELETE /{id} sem ownerUserId", delete(url)),
                Arguments.of("PATCH /{id}/status sem status", patch(url + "/status")
                        .contentType("application/json").content("{\"ownerUserId\":10}")),
                Arguments.of("PUT /{id}/horarios sem horarios", put(url + "/horarios")
                        .contentType("application/json").content("{\"ownerUserId\":10}")));
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
        verify(restauranteService, never()).deletarRestaurante(any(), any());
    }

    @Test
    void idNaoNumericoContinuaRetornando400SemConsultarORestaurante() throws Exception {
        mockMvc.perform(get("/api/restaurantes/abc")).andExpect(status().isBadRequest());
        verify(restauranteService, never()).verificarExistencia(any());
    }

    @Test
    void deleteRetorna204ESolicitaExclusaoLogica() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/5").param("ownerUserId", "10"))
                .andExpect(status().isNoContent());
        verify(restauranteService).deletarRestaurante(5L, 10L);
    }

    @Test
    void deleteSemOwnerUserIdRetorna400() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/5")).andExpect(status().isBadRequest());
    }

    @Test
    void statusPodeSerAlteradoPeloEndpointPatch() throws Exception {
        when(restauranteService.atualizarStatus(eq(5L), any(AtualizarStatusRestauranteDTO.class)))
                .thenReturn(resposta());
        mockMvc.perform(patch("/api/restaurantes/5/status")
                        .contentType("application/json")
                        .content("{\"ownerUserId\":10,\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isOk());
        verify(restauranteService).atualizarStatus(eq(5L), any(AtualizarStatusRestauranteDTO.class));
    }

    @Test
    void cadastroInvalidoRetorna400SemChamarService() throws Exception {
        mockMvc.perform(post("/api/restaurantes")
                        .contentType("application/json")
                        .content("{\"restauranteNome\":\" \",\"estado\":\"Pernambuco\",\"ownerUserId\":10}"))
                .andExpect(status().isBadRequest());
        verify(restauranteService, never()).cadastrarRestaurante(any());
    }

    @Test
    void statusSemValorRetorna400SemChamarService() throws Exception {
        mockMvc.perform(patch("/api/restaurantes/5/status")
                        .contentType("application/json")
                        .content("{\"ownerUserId\":10}"))
                .andExpect(status().isBadRequest());
        verify(restauranteService, never()).atualizarStatus(any(), any());
    }

    @Test
    void endpointRetornaHorariosDaSemana() throws Exception {
        when(restauranteService.listarHorarios(5L)).thenReturn(List.of());
        mockMvc.perform(get("/api/restaurantes/5/horarios")).andExpect(status().isOk());
        verify(restauranteService).listarHorarios(5L);
    }

    private RestauranteResponseDTO resposta() {
        return new RestauranteResponseDTO(5L, "Pizzaria", null, null, null, null, null, null, null, null, null,
                null, 10L, StatusRestaurante.FECHADO, CategoriaRestaurante.PIZZARIA, 0L, null, null, false);
    }
}
