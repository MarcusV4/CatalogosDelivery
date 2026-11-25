package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        RestauranteResponseDTO resposta = restauranteDTO();
        when(restauranteService.cadastrarRestaurante(any(RestauranteDTO.class))).thenReturn(salvo);
        when(restauranteMapper.toResponseDTO(salvo)).thenReturn(resposta);

        mockMvc.perform(post("/api/restaurantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restauranteNome":"Pizzaria Recife","cidade":"Recife","estado":"PE",
                                 "taxaEntrega":550,"ownerUserId":7,"statusRestaurante":"ABERTO",
                                 "categoriaRestaurante":"PIZZARIA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restauranteNome").value("Pizzaria Recife"));

        verify(restauranteService).cadastrarRestaurante(any(RestauranteDTO.class));
    }

    @Test
    void excluiRestauranteERetornaMensagemDeSucesso() throws Exception {
        mockMvc.perform(delete("/api/restaurantes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").value("Restaurante deletado com sucesso!"));

        verify(restauranteService).deletarRestaurante(10L);
    }

    private static RestauranteResponseDTO restauranteDTO() {
        return new RestauranteResponseDTO(10L, "Pizzaria Recife", "Pizza artesanal", "Recife", "PE",
                null, 7L, StatusRestaurante.ABERTO, CategoriaRestaurante.PIZZARIA, 550L);
    }
}
