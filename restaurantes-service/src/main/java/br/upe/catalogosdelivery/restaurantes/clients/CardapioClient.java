package br.upe.catalogosdelivery.restaurantes.clients;

import br.upe.catalogosdelivery.common.http.ClienteHttp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Acesso HTTP ao serviço de Cardápio (Restaurantes não acessa o banco dele). */
@Component
public class CardapioClient {

    private final ClienteHttp http;

    public CardapioClient(
            @Value("${catalogo.servicos.cardapio.url}") String url,
            @Value("${catalogo.servicos.timeout:3s}") Duration timeout) {
        this.http = ClienteHttp.criar("Restaurantes", "Cardápio", url, timeout);
    }

    public void excluirCardapioDoRestaurante(Long restauranteId) {
        http.delete("/api/cardapios/restaurante/{id}",
                "Cardápio do restaurante " + restauranteId + " não encontrado.", restauranteId);
    }
}
