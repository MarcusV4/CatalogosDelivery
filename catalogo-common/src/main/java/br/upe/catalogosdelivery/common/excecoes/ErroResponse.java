package br.upe.catalogosdelivery.common.excecoes;

/**
 * Formato de erro padrão da plataforma: {"erro": true, "mensagem": "..."}.
 */
public record ErroResponse(boolean erro, String mensagem) {

    public static ErroResponse de(String mensagem) {
        return new ErroResponse(true, mensagem);
    }
}
