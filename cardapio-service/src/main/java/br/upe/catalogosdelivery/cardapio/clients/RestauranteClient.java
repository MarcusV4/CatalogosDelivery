package br.upe.catalogosdelivery.cardapio.clients;

import br.upe.catalogosdelivery.common.http.ClienteHttp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Acesso HTTP ao serviço de Restaurantes (o Cardápio não acessa o banco dele). */
@Component
public class RestauranteClient {

    private final ClienteHttp http;

    public RestauranteClient(
            @Value("${catalogo.servicos.restaurantes.url}") String url,
            @Value("${catalogo.servicos.timeout:3s}") Duration timeout) {
        this.http = ClienteHttp.criar("Cardápio", "Restaurantes", url, timeout);
    }

    public RestauranteResumoDTO buscarRestaurante(Long restauranteId) {
        return http.get("/api/restaurantes/{id}", RestauranteResumoDTO.class,
                "Restaurante " + restauranteId + " não encontrado.", restauranteId);
    }
}
