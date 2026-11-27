package br.upe.catalogosdelivery.common.openapi;

import br.upe.catalogosdelivery.common.seguranca.RotaPublica;
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
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.Set;

/**
 * Documenta no Swagger quais rotas exigem o access token da Identidade: as escritas
 * sem {@link RotaPublica}. Elas ganham o cadeado ({@value #ESQUEMA}) e a resposta 401;
 * o botão "Authorize" do Swagger UI envia o token nessas chamadas.
 */
@Configuration
public class OpenApiSegurancaConfig {

    static final String ESQUEMA = "bearerAuth";
    private static final Set<RequestMethod> ESCRITAS =
            Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    @Bean
    public OpenApiCustomizer esquemaBearerDaIdentidade() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Access token da Identidade (Authorization Code + PKCE). "
                            + "Escritas de restaurante e cardápio exigem uma conta LOJA dona do restaurante."));
        };
    }

    @Bean
    public OperationCustomizer tokenNasEscritas() {
        return (operacao, handler) -> {
            if (exigeToken(handler)) {
                operacao.addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
                if (operacao.getResponses() == null) {
                    operacao.setResponses(new ApiResponses());
                }
                operacao.getResponses().putIfAbsent("401",
                        new ApiResponse().description("Sem token, ou token inválido, expirado ou revogado"));
            }
            return operacao;
        };
    }

    static boolean exigeToken(HandlerMethod handler) {
        if (AnnotatedElementUtils.hasAnnotation(handler.getMethod(), RotaPublica.class)
                || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), RotaPublica.class)) {
            return false;
        }
        RequestMapping mapeamento = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), RequestMapping.class);
        return mapeamento != null && Arrays.stream(mapeamento.method()).anyMatch(ESCRITAS::contains);
    }
}
