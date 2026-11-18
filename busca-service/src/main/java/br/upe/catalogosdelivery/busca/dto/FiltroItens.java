package br.upe.catalogosdelivery.busca.dto;

/** Filtros de GET /api/busca/itens; todos opcionais. Preços em centavos. */
public record FiltroItens(String nome, String categoria, Long precoMin, Long precoMax,
                          Boolean disponivel, Long restauranteId) {

    public static FiltroItens vazio() {
        return new FiltroItens(null, null, null, null, null, null);
    }
}
