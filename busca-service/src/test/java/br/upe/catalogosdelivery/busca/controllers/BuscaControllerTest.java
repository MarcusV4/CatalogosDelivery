package br.upe.catalogosdelivery.busca.controllers;

import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.busca.services.BuscaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuscaController.class)
class BuscaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscaService buscaService;

    @Test
    void encaminhaFiltrosDeRestauranteERetornaResultados() throws Exception {
        when(buscaService.buscarRestaurantes("pizza", "PIZZARIA", "ABERTO"))
                .thenReturn(List.of(new RestauranteDTO(10L, "Pizzaria Recife", null, "Recife", "PE",
                        null, "ABERTO", "PIZZARIA", 550L)));

        mockMvc.perform(get("/api/busca/restaurantes")
                        .param("nome", "pizza")
                        .param("categoria", "PIZZARIA")
                        .param("status", "ABERTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restauranteId").value(10))
                .andExpect(jsonPath("$[0].restauranteNome").value("Pizzaria Recife"));

        verify(buscaService).buscarRestaurantes("pizza", "PIZZARIA", "ABERTO");
    }

    @Test
    void encaminhaFiltrosDeItensERetornaResultados() throws Exception {
        when(buscaService.buscarItens("pizza", "PIZZA", 10L, true))
                .thenReturn(List.of(new ItemBuscaDTO(101L, "Pizza Recife", null, 2590L, true, "PIZZA",
                        10L, "Pizzaria Recife", "ABERTO")));

        mockMvc.perform(get("/api/busca/itens")
                        .param("nome", "pizza")
                        .param("categoria", "PIZZA")
                        .param("restauranteId", "10")
                        .param("disponivel", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].preco").value(2590))
                .andExpect(jsonPath("$[0].restauranteNome").value("Pizzaria Recife"));

        verify(buscaService).buscarItens("pizza", "PIZZA", 10L, true);
    }
}
