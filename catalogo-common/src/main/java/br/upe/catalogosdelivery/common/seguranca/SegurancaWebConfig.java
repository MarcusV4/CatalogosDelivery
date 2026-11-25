package br.upe.catalogosdelivery.common.seguranca;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Aplica o {@link AutenticacaoInterceptor} às rotas {@code /api/**} dos serviços do Catálogo. */
@Configuration
public class SegurancaWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<IdentidadeClient> identidade;

    public SegurancaWebConfig(ObjectProvider<IdentidadeClient> identidade) {
        this.identidade = identidade;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AutenticacaoInterceptor(() -> identidade.getObject()))
                .addPathPatterns("/api/**");
    }
}
