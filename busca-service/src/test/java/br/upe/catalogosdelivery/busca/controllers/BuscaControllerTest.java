package br.upe.catalogosdelivery.busca.controllers;

import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.FiltroItens;
import br.upe.catalogosdelivery.busca.dto.FiltroRestaurantes;
import br.upe.catalogosdelivery.busca.dto.PaginaDTO;
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
import static org.mockito.ArgumentMatchers.eq;
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
        FiltroRestaurantes filtro = new FiltroRestaurantes("pizza", "PIZZARIA", "ABERTO", null, null);
        when(buscaService.buscarRestaurantes(eq(filtro), eq(0), eq(20), eq(null)))
                .thenReturn(new PaginaDTO<>(List.of(new RestauranteDTO(10L, "Pizzaria Recife", null, "Recife", "PE",
                        null, "ABERTO", "PIZZARIA", 550L, 2000L, 30, 45, true)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/busca/restaurantes")
                        .param("nome", "pizza")
                        .param("categoria", "PIZZARIA")
                        .param("status", "ABERTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].restauranteId").value(10))
                .andExpect(jsonPath("$.conteudo[0].restauranteNome").value("Pizzaria Recife"))
                .andExpect(jsonPath("$.conteudo[0].pedidoMinimo").value(2000));

        verify(buscaService).buscarRestaurantes(filtro, 0, 20, null);
    }

    @Test
    void encaminhaFiltrosDeItensERetornaResultados() throws Exception {
        FiltroItens filtro = new FiltroItens("pizza", "PIZZA", null, null, true, 10L,
                null, null, null);
        when(buscaService.buscarItens(eq(filtro), eq(0), eq(20), eq(null)))
                .thenReturn(new PaginaDTO<>(List.of(new ItemBuscaDTO(101L, "Pizza Recife", null, 2590L,
                        true, "PIZZA", null, 10L, "Pizzaria Recife", "ABERTO", true)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/busca/itens")
                        .param("nome", "pizza")
                        .param("categoria", "PIZZA")
                        .param("restauranteId", "10")
                        .param("disponivel", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(101))
                .andExpect(jsonPath("$.conteudo[0].preco").value(2590))
                .andExpect(jsonPath("$.conteudo[0].restauranteNome").value("Pizzaria Recife"));

        verify(buscaService).buscarItens(filtro, 0, 20, null);
    }
}
