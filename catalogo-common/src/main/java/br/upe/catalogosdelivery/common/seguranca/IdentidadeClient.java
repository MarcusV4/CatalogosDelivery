package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;

/**
 * Validação do token junto ao serviço de Identidade (Equipe 1).
 * <p>
 * Fora do perfil {@code dev} a implementação é {@link IdentidadeHttpClient}, que chama a
 * Identidade por HTTP. No perfil {@code dev} é {@link IdentidadeMock}, enquanto a Equipe 1
 * não entrega o serviço.
 */
public interface IdentidadeClient {

    /**
     * @param token o JWT, sem o prefixo {@code Bearer}
     * @return o usuário dono do token
     * @throws NaoAutenticadoException      token inválido ou expirado (401)
     * @throws ServicoIndisponivelException Identidade fora do ar ou em timeout (503)
     */
    UsuarioAutenticado validarToken(String token);
}
