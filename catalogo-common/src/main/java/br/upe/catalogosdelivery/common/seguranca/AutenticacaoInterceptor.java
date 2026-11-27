package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Exige um access token válido nas escritas ({@code POST/PUT/PATCH/DELETE}) da API.
 * Leituras, o pre-flight de CORS e rotas com {@link RotaPublica} passam sem token.
 * O usuário validado fica num atributo da requisição, lido por {@link UsuarioAtual}.
 * <p>
 * Roda antes dos outros interceptors: uma escrita sem token recebe 401 antes de qualquer
 * 404 ou 400, para não revelar a quem não está logado quais restaurantes existem.
 */
public class AutenticacaoInterceptor implements HandlerInterceptor {

    static final String ATRIBUTO_USUARIO = AutenticacaoInterceptor.class.getName() + ".usuario";
    private static final Set<String> METODOS_DE_LEITURA = Set.of("GET", "HEAD", "OPTIONS");
    private static final String PREFIXO_BEARER = "Bearer ";

    private final ObjectProvider<ValidadorDeTokens> validador;

    public AutenticacaoInterceptor(ObjectProvider<ValidadorDeTokens> validador) {
        this.validador = validador;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request) || METODOS_DE_LEITURA.contains(request.getMethod())
                || !(handler instanceof HandlerMethod metodo) || ehRotaPublica(metodo)) {
            return true;
        }
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.regionMatches(true, 0, PREFIXO_BEARER, 0, PREFIXO_BEARER.length())
                || cabecalho.substring(PREFIXO_BEARER.length()).isBlank()) {
            throw new NaoAutenticadoException(
                    "Entre com sua conta para continuar (cabeçalho Authorization: Bearer <token>).");
        }
        ValidadorDeTokens disponivel = validador.getIfAvailable();
        if (disponivel == null) {
            throw new ServicoIndisponivelException("A validação de login não está disponível neste serviço.", null);
        }
        String token = cabecalho.substring(PREFIXO_BEARER.length()).trim();
        request.setAttribute(ATRIBUTO_USUARIO, disponivel.validar(token));
        return true;
    }

    static boolean ehRotaPublica(HandlerMethod metodo) {
        return AnnotatedElementUtils.hasAnnotation(metodo.getMethod(), RotaPublica.class)
                || AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), RotaPublica.class);
    }
}
