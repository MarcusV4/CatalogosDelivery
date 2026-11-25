package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * Responde 404 para restaurante inexistente antes de o Spring ler e validar o corpo ({@code @Valid})
 * e os parâmetros da requisição: em rotas com {@code {idRestaurante}}, um id inexistente nunca vira 400.
 * Id não numérico segue para o controller, que responde 400. Pre-flight de CORS não é verificado.
 */
@Component
public class RestauranteExistenteInterceptor implements HandlerInterceptor {

    /** Nome da variável de rota nos endpoints de {@link RestauranteController}. */
    static final String VARIAVEL_ID = "idRestaurante";

    private final RestauranteService restauranteService;

    public RestauranteExistenteInterceptor(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        Long id = idDaRota(request);
        if (id != null) {
            restauranteService.verificarExistencia(id);
        }
        return true;
    }

    /** Id numérico da rota, ou {@code null} se a rota não tem id ou ele não é um número. */
    private static Long idDaRota(HttpServletRequest request) {
        if (!(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> variaveis)
                || !(variaveis.get(VARIAVEL_ID) instanceof String valor)) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
