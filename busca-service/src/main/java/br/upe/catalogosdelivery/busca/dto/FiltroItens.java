package br.upe.catalogosdelivery.busca.dto;

/**
 * Filtros de GET /api/busca/itens; todos opcionais. Preços em centavos.
 * {@code categoriaRestaurante}, {@code abertoAgora} e {@code entregaGratis} se aplicam ao restaurante do item.
 */
public record FiltroItens(String nome, String categoria, Long precoMin, Long precoMax,
                          Boolean disponivel, Long restauranteId,
                          String categoriaRestaurante, Boolean abertoAgora, Boolean entregaGratis) {

    public static FiltroItens vazio() {
        return new FiltroItens(null, null, null, null, null, null, null, null, null);
    }

    /** Se algum filtro depende dos dados do serviço de Restaurantes. */
    public boolean filtraPeloRestaurante() {
        return categoriaRestaurante != null && !categoriaRestaurante.isBlank()
                || abertoAgora != null
                || entregaGratis != null;
    }
}
