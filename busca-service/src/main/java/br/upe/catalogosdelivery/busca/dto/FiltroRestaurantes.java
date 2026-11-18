package br.upe.catalogosdelivery.busca.dto;

/** Filtros de GET /api/busca/restaurantes; todos opcionais. */
public record FiltroRestaurantes(String nome, String categoria, String status, Boolean abertoAgora) {

    public static FiltroRestaurantes vazio() {
        return new FiltroRestaurantes(null, null, null, null);
    }
}
