package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Dá aos services o usuário da requisição atual, já validado pelo {@link AutenticacaoInterceptor}. */
@Component
public class UsuarioAtual {

    private final List<String> papeisLoja;

    public UsuarioAtual(IdentidadeProperties config) {
        this.papeisLoja = config.papeisLoja();
    }

    public UsuarioAutenticado obter() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        Object usuario = atributos == null ? null
                : atributos.getAttribute(AutenticacaoInterceptor.ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST);
        if (usuario instanceof UsuarioAutenticado autenticado) {
            return autenticado;
        }
        throw new NaoAutenticadoException("Entre com sua conta para continuar.");
    }

    /** Usuário atual, desde que seja uma conta de loja (papel {@code LOJA}); senão, 403. */
    public UsuarioAutenticado obterLoja() {
        UsuarioAutenticado usuario = obter();
        if (!usuario.possuiAlgumPapel(papeisLoja)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente contas de loja podem gerenciar restaurantes e cardápios.");
        }
        return usuario;
    }
}
