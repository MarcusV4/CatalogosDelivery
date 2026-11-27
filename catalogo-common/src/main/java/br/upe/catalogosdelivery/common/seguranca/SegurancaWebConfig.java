package br.upe.catalogosdelivery.common.seguranca;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra o {@link AutenticacaoInterceptor} nas rotas da API, antes de qualquer outro. */
@Configuration
public class SegurancaWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<ValidadorDeTokens> validador;

    public SegurancaWebConfig(ObjectProvider<ValidadorDeTokens> validador) {
        this.validador = validador;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AutenticacaoInterceptor(validador))
                .addPathPatterns("/api/**")
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
