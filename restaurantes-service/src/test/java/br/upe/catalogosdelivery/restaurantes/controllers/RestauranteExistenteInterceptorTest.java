package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.restaurantes.exceptions.RestauranteNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RestauranteExistenteInterceptorTest {

    private RestauranteService restauranteService;
    private RestauranteExistenteInterceptor interceptor;

    @BeforeEach
    void configurar() {
        restauranteService = mock(RestauranteService.class);
        interceptor = new RestauranteExistenteInterceptor(restauranteService);
    }

    private static MockHttpServletRequest requisicao(String metodo, Map<String, String> variaveisDaRota) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, "/api/restaurantes/qualquer");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, variaveisDaRota);
        return request;
    }

    @Test
    void verificaORestauranteDaRota() {
        assertTrue(interceptor.preHandle(requisicao("PUT", Map.of("idRestaurante", "5")),
                new MockHttpServletResponse(), new Object()));
        verify(restauranteService).verificarExistencia(5L);
    }

    @Test
    void propagaO404DeRestauranteInexistente() {
        doThrow(new RestauranteNaoEncontradoException(99999L)).when(restauranteService).verificarExistencia(99999L);
        assertThrows(RestauranteNaoEncontradoException.class, () -> interceptor.preHandle(
                requisicao("DELETE", Map.of("idRestaurante", "99999")), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void deixaIdNaoNumericoParaOController() {
        assertTrue(interceptor.preHandle(requisicao("GET", Map.of("idRestaurante", "abc")),
                new MockHttpServletResponse(), new Object()));
        verify(restauranteService, never()).verificarExistencia(any());
    }

    @Test
    void ignoraRotasSemId() {
        assertTrue(interceptor.preHandle(requisicao("GET", Map.of()), new MockHttpServletResponse(), new Object()));
        verify(restauranteService, never()).verificarExistencia(any());
    }

    @Test
    void ignoraPreFlightDeCors() {
        MockHttpServletRequest preflight = requisicao("OPTIONS", Map.of("idRestaurante", "99999"));
        preflight.addHeader("Origin", "http://localhost:5173");
        preflight.addHeader("Access-Control-Request-Method", "PUT");
        assertTrue(interceptor.preHandle(preflight, new MockHttpServletResponse(), new Object()));
        verify(restauranteService, never()).verificarExistencia(any());
    }
}
