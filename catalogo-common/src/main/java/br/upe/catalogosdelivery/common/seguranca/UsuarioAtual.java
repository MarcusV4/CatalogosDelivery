package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Dá acesso ao usuário autenticado da requisição atual, gravado pelo
 * {@link AutenticacaoInterceptor} depois de validar o token na Identidade.
 */
@Component
public class UsuarioAtual {

    static final String ATRIBUTO = UsuarioAtual.class.getName() + ".USUARIO";

    /** @throws NaoAutenticadoException se a requisição não passou pela autenticação */
    public UsuarioAutenticado obter() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        Object usuario = atributos == null ? null
                : atributos.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        if (usuario instanceof UsuarioAutenticado autenticado) {
            return autenticado;
        }
        throw new NaoAutenticadoException("Token de autenticação ausente. Envie o header Authorization: Bearer <token>.");
    }
}
