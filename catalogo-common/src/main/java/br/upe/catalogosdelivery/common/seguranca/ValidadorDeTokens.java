package br.upe.catalogosdelivery.common.seguranca;

/**
 * Valida o access token de uma requisição de escrita.
 * <p>
 * Lança {@code NaoAutenticadoException} (401) para token inválido, expirado ou revogado, e
 * {@code ServicoIndisponivelException} (503) quando a Identidade não pode ser consultada:
 * na dúvida, a escrita é recusada (falha fechada).
 */
public interface ValidadorDeTokens {

    UsuarioAutenticado validar(String accessToken);
}
