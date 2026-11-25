package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.common.excecoes.GlobalExceptionHandler;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import br.upe.catalogosdelivery.common.seguranca.AutenticacaoInterceptor;
import br.upe.catalogosdelivery.common.seguranca.IdentidadeClient;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAtual;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestauranteControllerTest {

    private static final String TOKEN = "Bearer token-do-dono";

    private RestauranteService restauranteService;
    private IdentidadeClient identidade;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        restauranteService = mock(RestauranteService.class);
        identidade = mock(IdentidadeClient.class);
        when(identidade.validarToken("token-do-dono")).thenReturn(new UsuarioAutenticado(10L, "LOJISTA"));
        mockMvc = MockMvcBuilders.standaloneSetup(new RestauranteController(restauranteService,
                        new RestauranteMapper(), new UsuarioAtual()))
                .addInterceptors(new AutenticacaoInterceptor(identidade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deleteUsaOSubDoTokenESolicitaExclusaoLogica() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/5").header("Authorization", TOKEN))
                .andExpect(status().isNoContent());
        verify(restauranteService).deletarRestaurante(5L, 10L);
    }

    @Test
    void ownerUserIdNaQueryNaoSubstituiOToken() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/5").param("ownerUserId", "99").header("Authorization", TOKEN))
                .andExpect(status().isNoContent());
        verify(restauranteService).deletarRestaurante(5L, 10L);
    }

    @Test
    void escritasSemTokenRetornam401SemChamarService() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/5")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value(true));
        mockMvc.perform(post("/api/restaurantes").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/restaurantes/5/horarios").contentType("application/json").content("{\"horarios\":[]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(restauranteService, identidade);
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        when(identidade.validarToken("forjado")).thenThrow(new NaoAutenticadoException("Token inválido ou expirado."));
        mockMvc.perform(patch("/api/restaurantes/5/status").header("Authorization", "Bearer forjado")
                        .contentType("application/json").content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado."));
        verifyNoInteractions(restauranteService);
    }

    @Test
    void quemNaoEhDonoRecebe403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não é proprietário do restaurante."))
                .when(restauranteService).deletarRestaurante(5L, 10L);
        mockMvc.perform(delete("/api/restaurantes/5").header("Authorization", TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Usuário não é proprietário do restaurante."));
    }

    @Test
    void identidadeForaDoArRetorna503() throws Exception {
        when(identidade.validarToken("token-do-dono"))
                .thenThrow(new ServicoIndisponivelException("Serviço de identidade indisponível", null));
        mockMvc.perform(delete("/api/restaurantes/5").header("Authorization", TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value(true))
                .andExpect(jsonPath("$.mensagem").value("Serviço de identidade indisponível"));
        verify(restauranteService, never()).deletarRestaurante(anyLong(), anyLong());
        // As leituras seguem públicas e funcionando.
        when(restauranteService.listarHorarios(5L)).thenReturn(List.of());
        mockMvc.perform(get("/api/restaurantes/5/horarios")).andExpect(status().isOk());
    }

    @Test
    void statusPodeSerAlteradoPeloEndpointPatch() throws Exception {
        when(restauranteService.atualizarStatus(eq(5L), any(AtualizarStatusRestauranteDTO.class), eq(10L)))
                .thenReturn(resposta());
        mockMvc.perform(patch("/api/restaurantes/5/status").header("Authorization", TOKEN)
                        .contentType("application/json")
                        .content("{\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isOk());
        verify(restauranteService).atualizarStatus(eq(5L), any(AtualizarStatusRestauranteDTO.class), eq(10L));
    }

    @Test
    void cadastroInvalidoRetorna400SemChamarService() throws Exception {
        mockMvc.perform(post("/api/restaurantes").header("Authorization", TOKEN)
                        .contentType("application/json")
                        .content("{\"restauranteNome\":\" \",\"estado\":\"Pernambuco\"}"))
                .andExpect(status().isBadRequest());
        verify(restauranteService, never()).cadastrarRestaurante(any(), any());
    }

    @Test
    void statusSemValorRetorna400SemChamarService() throws Exception {
        mockMvc.perform(patch("/api/restaurantes/5/status").header("Authorization", TOKEN)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(restauranteService, never()).atualizarStatus(any(), any(), any());
    }

    @Test
    void leiturasNaoExigemToken() throws Exception {
        when(restauranteService.listarHorarios(5L)).thenReturn(List.of());
        when(restauranteService.listarRestaurante()).thenReturn(List.of());
        mockMvc.perform(get("/api/restaurantes/5/horarios")).andExpect(status().isOk());
        mockMvc.perform(get("/api/restaurantes")).andExpect(status().isOk());
        verify(restauranteService).listarHorarios(5L);
        verifyNoInteractions(identidade);
    }

    private RestauranteResponseDTO resposta() {
        return new RestauranteResponseDTO(5L, "Pizzaria", null, null, null, null, null, null, null, null, null,
                null, 10L, StatusRestaurante.FECHADO, CategoriaRestaurante.PIZZARIA, 0L, null, null, false);
    }
}
