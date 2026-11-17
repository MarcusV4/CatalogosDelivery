package br.upe.catalogosdelivery.common.openapi;

import br.upe.catalogosdelivery.common.excecoes.ErroResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sem isto, o springdoc documenta respostas 4xx/5xx com o mesmo schema do retorno de
 * sucesso (ex.: um 404 "devolvendo" RestauranteResponseDTO). Como o GlobalExceptionHandler
 * responde todo erro com {@link ErroResponse}, este customizador aplica esse schema a todas
 * as respostas de erro dos 3 serviços, inclusive de endpoints criados no futuro.
 */
@Configuration
public class OpenApiErroConfig {

    static final String SCHEMA_ERRO = ErroResponse.class.getSimpleName();

    @Bean
    public OpenApiCustomizer respostasDeErroNoFormatoPadrao() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            ModelConverters.getInstance().read(ErroResponse.class)
                    .forEach(openApi.getComponents()::addSchemas);

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operacao -> {
                if (operacao.getResponses() == null) {
                    return;
                }
                operacao.getResponses().forEach((codigo, resposta) -> {
                    if (codigo.startsWith("4") || codigo.startsWith("5")) {
                        resposta.setContent(new Content().addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + SCHEMA_ERRO))));
                    }
                });
            }));
        };
    }
}
