package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Exige {@code Authorization: Bearer <token>} nas rotas de escrita e valida o token na
 * Identidade antes de chegar ao controller. O usuário validado fica disponível em
 * {@link UsuarioAtual}. Rotas GET (listar, buscar, cardápio) continuam públicas.
 * <p>
 * Roda dentro do DispatcherServlet, então as exceções lançadas aqui passam pelo
 * GlobalExceptionHandler e saem no formato de erro da plataforma (401/503).
 */
public class AutenticacaoInterceptor implements HandlerInterceptor {

    static final Set<String> METODOS_DE_ESCRITA = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String PREFIXO_BEARER = "bearer ";

    private final Supplier<IdentidadeClient> identidade;

    public AutenticacaoInterceptor(IdentidadeClient identidade) {
        this(() -> identidade);
    }

    /** O client é resolvido só na primeira escrita, para não exigir o bean em testes que só leem. */
    public AutenticacaoInterceptor(Supplier<IdentidadeClient> identidade) {
        this.identidade = identidade;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Preflight de CORS e recursos estáticos não são HandlerMethod e passam direto.
        if (!(handler instanceof HandlerMethod metodo) || !exigeAutenticacao(request.getMethod(), metodo)) {
            return true;
        }
        UsuarioAutenticado usuario = identidade.get().validarToken(extrairToken(request));
        request.setAttribute(UsuarioAtual.ATRIBUTO, usuario);
        return true;
    }

    /** Escritas exigem token, a não ser que o endpoint (ou o controller) seja {@link RotaPublica}. */
    public static boolean exigeAutenticacao(String metodoHttp, HandlerMethod metodo) {
        return metodoHttp != null
                && METODOS_DE_ESCRITA.contains(metodoHttp.toUpperCase(Locale.ROOT))
                && !metodo.hasMethodAnnotation(RotaPublica.class)
                && !AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), RotaPublica.class);
    }

    private static String extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            throw new NaoAutenticadoException("Token de autenticação ausente. Envie o header Authorization: Bearer <token>.");
        }
        if (!header.toLowerCase(Locale.ROOT).startsWith(PREFIXO_BEARER)
                || header.substring(PREFIXO_BEARER.length()).isBlank()) {
            throw new NaoAutenticadoException("Header Authorization inválido. Use o formato Bearer <token>.");
        }
        return header.substring(PREFIXO_BEARER.length()).trim();
    }
}
