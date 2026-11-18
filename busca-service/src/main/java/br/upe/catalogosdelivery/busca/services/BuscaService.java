package br.upe.catalogosdelivery.busca.services;

import br.upe.catalogosdelivery.busca.clients.CardapioClient;
import br.upe.catalogosdelivery.busca.clients.RestauranteClient;
import br.upe.catalogosdelivery.busca.dto.FiltroItens;
import br.upe.catalogosdelivery.busca.dto.FiltroRestaurantes;
import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.PaginaDTO;
import br.upe.catalogosdelivery.busca.dto.ProdutoDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Filtros, ordenação e paginação sobre os dados dos serviços de Restaurantes e Cardápio.
 * A Busca não tem banco: a cada requisição lê as listas completas via HTTP e filtra em memória.
 */
@Service
@RequiredArgsConstructor
public class BuscaService {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private static final Logger log = LoggerFactory.getLogger(BuscaService.class);

    private static final Ordenacao<RestauranteDTO> ORDENACAO_RESTAURANTES =
            new Ordenacao<RestauranteDTO>("nome",
                    Comparator.comparing(RestauranteDTO::restauranteId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .campo("nome", r -> normalizarOuNulo(r.restauranteNome()))
                    .campo("categoria", RestauranteDTO::categoriaRestaurante)
                    .campo("taxaEntrega", RestauranteDTO::taxaEntrega)
                    .campo("tempoEstimado", RestauranteDTO::tempoEstimadoMin);

    private static final Ordenacao<ItemBuscaDTO> ORDENACAO_ITENS =
            new Ordenacao<ItemBuscaDTO>("nome",
                    Comparator.comparing(ItemBuscaDTO::id, Comparator.nullsLast(Comparator.naturalOrder())))
                    .campo("nome", i -> normalizarOuNulo(i.nome()))
                    .campo("preco", ItemBuscaDTO::preco)
                    .campo("categoria", ItemBuscaDTO::categoria);

    private final RestauranteClient restauranteClient;
    private final CardapioClient cardapioClient;

    public PaginaDTO<RestauranteDTO> buscarRestaurantes(FiltroRestaurantes filtro, int pagina, int tamanho,
                                                        String sort) {
        validarPaginacao(pagina, tamanho);
        Comparator<RestauranteDTO> ordem = ORDENACAO_RESTAURANTES.comparador(sort);

        List<RestauranteDTO> resultado = restauranteClient.listarRestaurantes().stream()
                .filter(r -> contemEmAlgum(filtro.nome(), r.restauranteNome(), r.restauranteDesc()))
                .filter(r -> igual(r.statusRestaurante(), filtro.status()))
                .filter(r -> atendeFiltrosDeRestaurante(r, filtro.categoria(), filtro.abertoAgora(),
                        filtro.entregaGratis()))
                .sorted(ordem)
                .toList();
        return PaginaDTO.de(resultado, pagina, tamanho);
    }

    public PaginaDTO<ItemBuscaDTO> buscarItens(FiltroItens filtro, int pagina, int tamanho, String sort) {
        validarPaginacao(pagina, tamanho);
        validarFaixaDePreco(filtro.precoMin(), filtro.precoMax());
        Comparator<ItemBuscaDTO> ordem = ORDENACAO_ITENS.comparador(sort);

        List<ProdutoDTO> produtos = cardapioClient.listarProdutos().stream()
                .filter(p -> contemEmAlgum(filtro.nome(), p.nome(), p.descricao()))
                .filter(p -> igual(p.categoria(), filtro.categoria()))
                .filter(p -> filtro.restauranteId() == null || filtro.restauranteId().equals(p.restauranteId()))
                .filter(p -> filtro.disponivel() == null || filtro.disponivel() == p.disponivel())
                .filter(p -> filtro.precoMin() == null || p.preco() != null && p.preco() >= filtro.precoMin())
                .filter(p -> filtro.precoMax() == null || p.preco() != null && p.preco() <= filtro.precoMax())
                .toList();

        Optional<Map<Long, RestauranteDTO>> restaurantes = restaurantesPorId(produtos, filtro.filtraPeloRestaurante());

        List<ItemBuscaDTO> resultado = produtos.stream()
                // Item de restaurante que não existe mais (ou foi desativado) não aparece. Se Restaurantes
                // estiver fora do ar não dá para saber, então o item aparece sem os dados do restaurante.
                .filter(p -> restaurantes.isEmpty()
                        || restauranteDoItemAtende(restaurantes.get().get(p.restauranteId()), filtro))
                .map(p -> paraItem(p, restaurantes.map(m -> m.get(p.restauranteId())).orElse(null)))
                .sorted(ordem)
                .toList();
        return PaginaDTO.de(resultado, pagina, tamanho);
    }

    private static ItemBuscaDTO paraItem(ProdutoDTO p, RestauranteDTO r) {
        return new ItemBuscaDTO(p.id(), p.nome(), p.descricao(), p.preco(), p.disponivel(), p.categoria(),
                p.urlImagem(), p.restauranteId(),
                r == null ? null : r.restauranteNome(),
                r == null ? null : r.statusRestaurante(),
                r == null ? null : Boolean.TRUE.equals(r.abertoAgora()));
    }

    private static boolean restauranteDoItemAtende(RestauranteDTO r, FiltroItens filtro) {
        return r != null && atendeFiltrosDeRestaurante(r, filtro.categoriaRestaurante(), filtro.abertoAgora(),
                filtro.entregaGratis());
    }

    /** Filtros comuns às duas rotas que olham o restaurante; parâmetros nulos não filtram. */
    private static boolean atendeFiltrosDeRestaurante(RestauranteDTO r, String categoria, Boolean abertoAgora,
                                                      Boolean entregaGratis) {
        return igual(r.categoriaRestaurante(), categoria)
                && (abertoAgora == null || abertoAgora == Boolean.TRUE.equals(r.abertoAgora()))
                && (entregaGratis == null || entregaGratis == temEntregaGratis(r));
    }

    private static boolean temEntregaGratis(RestauranteDTO r) {
        return r.taxaEntrega() != null && r.taxaEntrega() == 0;
    }

    /**
     * Restaurantes existentes por id, ou vazio se o serviço de Restaurantes estiver fora do ar
     * (nesse caso a busca de itens continua, só sem os dados do restaurante). Se a busca usa
     * filtros do restaurante, não há como aplicá-los e a indisponibilidade é propagada (503).
     */
    private Optional<Map<Long, RestauranteDTO>> restaurantesPorId(List<ProdutoDTO> produtos,
                                                                  boolean filtraPeloRestaurante) {
        if (produtos.isEmpty()) {
            return Optional.of(Map.of());
        }
        try {
            return Optional.of(restauranteClient.listarRestaurantes().stream()
                    .collect(Collectors.toMap(RestauranteDTO::restauranteId, Function.identity(), (a, b) -> a)));
        } catch (ServicoIndisponivelException e) {
            if (filtraPeloRestaurante) {
                throw e;
            }
            log.warn("[Busca] Restaurantes indisponível: retornando itens sem os dados do restaurante");
            return Optional.empty();
        }
    }

    private static void validarPaginacao(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page deve ser maior ou igual a 0.");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size deve estar entre 1 e " + TAMANHO_MAXIMO + ".");
        }
    }

    private static void validarFaixaDePreco(Long precoMin, Long precoMax) {
        if (precoMin != null && precoMin < 0 || precoMax != null && precoMax < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "precoMin e precoMax são em centavos e não podem ser negativos.");
        }
        if (precoMin != null && precoMax != null && precoMin > precoMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "precoMin não pode ser maior que precoMax.");
        }
    }

    /** Se o termo aparece em algum dos textos (nome, descrição...); termo vazio não filtra. */
    private static boolean contemEmAlgum(String termo, String... valores) {
        if (termo == null || termo.isBlank()) {
            return true;
        }
        String termoNormalizado = normalizar(termo);
        return Arrays.stream(valores)
                .anyMatch(valor -> valor != null && normalizar(valor).contains(termoNormalizado));
    }

    private static boolean igual(String valor, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        return valor != null && normalizar(valor).equals(normalizar(filtro));
    }

    private static String normalizarOuNulo(String texto) {
        return texto == null ? null : normalizar(texto);
    }

    /** Ignora maiúsculas/minúsculas e acentos: "acai" encontra "Açaí". */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
