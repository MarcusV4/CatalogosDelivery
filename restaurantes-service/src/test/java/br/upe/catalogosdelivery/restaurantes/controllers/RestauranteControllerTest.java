package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.HorarioFuncionamentoDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestauranteController.class)
class RestauranteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestauranteService restauranteService;

    @MockitoBean
    private RestauranteMapper restauranteMapper;

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
                .andExpect(jsonPath("$.categoriaRestaurante").value("PIZZARIA"));
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
                                 "ownerUserId":7,"categoriaRestaurante":"PIZZARIA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restauranteNome").value("Pizzaria Recife"));

        verify(restauranteService).cadastrarRestaurante(any(RestauranteDTO.class));
    }

    @Test
    void excluiRestauranteLogicamenteERetorna204() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/10").param("ownerUserId", "7"))
                .andExpect(status().isNoContent());

        verify(restauranteService).deletarRestaurante(10L, 7L);
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerUserId\":10,\"statusRestaurante\":\"FECHADO\"}"))
                .andExpect(status().isOk());

        verify(restauranteService).atualizarStatus(eq(5L), any(AtualizarStatusRestauranteDTO.class));
    }

    @Test
    void cadastroInvalidoRetorna400SemChamarService() throws Exception {
        mockMvc.perform(post("/api/restaurantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restauranteNome\":\" \",\"estado\":\"Pernambuco\",\"ownerUserId\":10}"))
                .andExpect(status().isBadRequest());

        verify(restauranteService, never()).cadastrarRestaurante(any());
    }

    @Test
    void statusSemValorRetorna400SemChamarService() throws Exception {
        mockMvc.perform(patch("/api/restaurantes/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerUserId\":10}"))
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
                "Centro", "Recife", "PE", "50000-000", "81999999999", null, 7L,
                StatusRestaurante.ABERTO, CategoriaRestaurante.PIZZARIA, 550L, 30, 45, true);
    }

    private static RestauranteResponseDTO resposta() {
        return new RestauranteResponseDTO(5L, "Pizzaria", null, null, null, null, null, null, null, null,
                null, null, 10L, StatusRestaurante.FECHADO, CategoriaRestaurante.PIZZARIA, 0L,
                null, null, false);
    }
}
