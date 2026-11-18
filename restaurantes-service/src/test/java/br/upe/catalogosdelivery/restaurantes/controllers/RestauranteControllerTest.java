package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestauranteControllerTest {

    private RestauranteService restauranteService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        restauranteService = mock(RestauranteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RestauranteController(restauranteService,
                new RestauranteMapper())).build();
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
