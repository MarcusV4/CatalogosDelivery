package br.upe.catalogosdelivery.busca.clients;

import br.upe.catalogosdelivery.busca.dto.ProdutoDTO;
import br.upe.catalogosdelivery.common.http.ClienteHttp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Acesso HTTP ao serviço de Cardápio. */
@Component
public class CardapioClient {

    private final ClienteHttp http;

    public CardapioClient(
            @Value("${catalogo.servicos.cardapio.url}") String url,
            @Value("${catalogo.servicos.timeout:3s}") Duration timeout) {
        this.http = ClienteHttp.criar("Busca", "Cardápio", url, timeout);
    }

    public List<ProdutoDTO> listarProdutos() {
        List<ProdutoDTO> produtos = http.get("/api/produtos",
                new ParameterizedTypeReference<List<ProdutoDTO>>() {}, "Produtos não encontrados.");
        return produtos == null ? List.of() : produtos;
    }
}
