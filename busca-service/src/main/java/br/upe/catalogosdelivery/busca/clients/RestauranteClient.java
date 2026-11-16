package br.upe.catalogosdelivery.busca.clients;

import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.common.http.ClienteHttp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Acesso HTTP ao serviço de Restaurantes. */
@Component
public class RestauranteClient {

    private final ClienteHttp http;

    public RestauranteClient(
            @Value("${catalogo.servicos.restaurantes.url}") String url,
            @Value("${catalogo.servicos.timeout:3s}") Duration timeout) {
        this.http = ClienteHttp.criar("Busca", "Restaurantes", url, timeout);
    }

    public List<RestauranteDTO> listarRestaurantes() {
        List<RestauranteDTO> restaurantes = http.get("/api/restaurantes",
                new ParameterizedTypeReference<List<RestauranteDTO>>() {}, "Restaurantes não encontrados.");
        return restaurantes == null ? List.of() : restaurantes;
    }
}
