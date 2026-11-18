package br.upe.catalogosdelivery.busca.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Uma página do resultado da busca. */
public record PaginaDTO<T>(
        @Schema(description = "Itens desta página") List<T> conteudo,
        @Schema(description = "Número da página, começando em 0", example = "0") int pagina,
        @Schema(description = "Tamanho de página pedido", example = "20") int tamanho,
        @Schema(description = "Total de resultados com os filtros aplicados", example = "42") long totalElementos,
        @Schema(description = "Total de páginas", example = "3") int totalPaginas
) {

    /** Recorta a página {@code pagina} (base 0) de uma lista já filtrada e ordenada. */
    public static <T> PaginaDTO<T> de(List<T> todos, int pagina, int tamanho) {
        int total = todos.size();
        int inicio = (int) Math.min((long) pagina * tamanho, total);
        int fim = Math.min(inicio + tamanho, total);
        int totalPaginas = (int) Math.ceil((double) total / tamanho);
        return new PaginaDTO<>(List.copyOf(todos.subList(inicio, fim)), pagina, tamanho, total, totalPaginas);
    }
}
