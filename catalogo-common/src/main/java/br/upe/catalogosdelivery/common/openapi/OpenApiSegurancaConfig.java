package br.upe.catalogosdelivery.common.openapi;

import br.upe.catalogosdelivery.common.seguranca.AutenticacaoInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;

/**
 * Documenta no Swagger a autenticação das rotas de escrita: o esquema {@code bearerAuth}
 * (botão "Authorize") e, em cada operação protegida, o cadeado e as respostas 401, 403 e 503.
 * Usa a mesma regra do {@link AutenticacaoInterceptor}, então vale para endpoints futuros.
 */
@Configuration
public class OpenApiSegurancaConfig {

    static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenApiCustomizer esquemaBearer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT emitido pelo serviço de Identidade (Equipe 1). No perfil dev, "
                            + "qualquer JWT com as claims sub e role é aceito, sem verificar a assinatura."));
        };
    }

    @Bean
    public OperationCustomizer rotasProtegidas() {
        return (operacao, metodo) -> {
            RequestMapping mapeamento = AnnotatedElementUtils.findMergedAnnotation(metodo.getMethod(), RequestMapping.class);
            boolean protegida = mapeamento != null && Arrays.stream(mapeamento.method())
                    .map(RequestMethod::name)
                    .anyMatch(http -> AutenticacaoInterceptor.exigeAutenticacao(http, metodo));
            if (!protegida) {
                return operacao;
            }
            operacao.addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
            if (operacao.getResponses() == null) {
                operacao.setResponses(new ApiResponses());
            }
            ApiResponses respostas = operacao.getResponses();
            respostas.putIfAbsent("401", new ApiResponse().description("Token ausente ou inválido"));
            respostas.putIfAbsent("403", new ApiResponse().description("Usuário sem permissão para esta operação"));
            respostas.putIfAbsent("503", new ApiResponse().description("Serviço de identidade indisponível"));
            return operacao;
        };
    }
}
