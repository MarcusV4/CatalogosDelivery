package br.upe.catalogosdelivery.common.excecoes;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Formato de erro padrão da plataforma: {"erro": true, "mensagem": "..."}.
 */
@Schema(description = "Formato de erro padrão da plataforma")
public record ErroResponse(
        @Schema(description = "Sempre true em respostas de erro", example = "true") boolean erro,
        @Schema(description = "Descrição do erro", example = "Error: Restaurante não encontrado") String mensagem) {

    public static ErroResponse de(String mensagem) {
        return new ErroResponse(true, mensagem);
    }
}
