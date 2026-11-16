package br.upe.catalogosdelivery.busca.services;

import br.upe.catalogosdelivery.busca.clients.CardapioClient;
import br.upe.catalogosdelivery.busca.clients.RestauranteClient;
import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.ProdutoDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Filtros básicos sobre os dados dos serviços de Restaurantes e Cardápio.
 * Paginação, faixa de preço e "aberto agora" ficam para a issue #29.
 */
@Service
@RequiredArgsConstructor
public class BuscaService {

    private static final Logger log = LoggerFactory.getLogger(BuscaService.class);

    private final RestauranteClient restauranteClient;
    private final CardapioClient cardapioClient;

    public List<RestauranteDTO> buscarRestaurantes(String nome, String categoria, String status) {
        return restauranteClient.listarRestaurantes().stream()
                .filter(r -> contem(r.restauranteNome(), nome))
                .filter(r -> igual(r.categoriaRestaurante(), categoria))
                .filter(r -> igual(r.statusRestaurante(), status))
                .toList();
    }

    public List<ItemBuscaDTO> buscarItens(String nome, String categoria, Long restauranteId, Boolean disponivel) {
        List<ProdutoDTO> produtos = cardapioClient.listarProdutos().stream()
                .filter(p -> contem(p.nome(), nome))
                .filter(p -> igual(p.categoria(), categoria))
                .filter(p -> restauranteId == null || restauranteId.equals(p.restauranteId()))
                .filter(p -> disponivel == null || disponivel == p.disponivel())
                .toList();

        Optional<Map<Long, RestauranteDTO>> restaurantes = restaurantesPorId(produtos);

        return produtos.stream()
                // Item de restaurante que não existe mais não aparece. Se Restaurantes estiver
                // fora do ar não dá para saber, então o item aparece sem os dados do restaurante.
                .filter(p -> restaurantes.isEmpty() || restaurantes.get().containsKey(p.restauranteId()))
                .map(p -> {
            RestauranteDTO r = restaurantes.map(m -> m.get(p.restauranteId())).orElse(null);
            return new ItemBuscaDTO(p.id(), p.nome(), p.descricao(), p.preco(), p.disponivel(), p.categoria(),
                    p.restauranteId(),
                    r == null ? null : r.restauranteNome(),
                    r == null ? null : r.statusRestaurante());
        }).toList();
    }

    /**
     * Restaurantes existentes por id, ou vazio se o serviço de Restaurantes estiver fora do ar
     * (nesse caso a busca de itens continua, só sem os dados do restaurante).
     */
    private Optional<Map<Long, RestauranteDTO>> restaurantesPorId(List<ProdutoDTO> produtos) {
        if (produtos.isEmpty()) {
            return Optional.of(Map.of());
        }
        try {
            return Optional.of(restauranteClient.listarRestaurantes().stream()
                    .collect(Collectors.toMap(RestauranteDTO::restauranteId, Function.identity(), (a, b) -> a)));
        } catch (ServicoIndisponivelException e) {
            log.warn("[Busca] Restaurantes indisponível: retornando itens sem os dados do restaurante");
            return Optional.empty();
        }
    }

    private static boolean contem(String valor, String termo) {
        if (termo == null || termo.isBlank()) {
            return true;
        }
        return valor != null && normalizar(valor).contains(normalizar(termo));
    }

    private static boolean igual(String valor, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        return valor != null && normalizar(valor).equals(normalizar(filtro));
    }

    /** Ignora maiúsculas/minúsculas e acentos: "acai" encontra "Açaí". */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
