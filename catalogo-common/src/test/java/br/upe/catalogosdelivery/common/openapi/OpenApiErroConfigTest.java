package br.upe.catalogosdelivery.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiErroConfigTest {

    @Test
    void respostasDeErroUsamErroResponseESucessoNaoMuda() {
        Content sucesso = new Content().addMediaType("*/*",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/RestauranteResponseDTO")));
        ApiResponses respostas = new ApiResponses()
                .addApiResponse("200", new ApiResponse().content(sucesso))
                .addApiResponse("404", new ApiResponse().content(sucesso))
                .addApiResponse("503", new ApiResponse());
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/restaurantes/{id}", new PathItem().get(new Operation().responses(respostas))));

        new OpenApiErroConfig().respostasDeErroNoFormatoPadrao().customise(openApi);

        assertEquals("#/components/schemas/RestauranteResponseDTO",
                respostas.get("200").getContent().get("*/*").getSchema().get$ref());
        assertEquals("#/components/schemas/ErroResponse",
                respostas.get("404").getContent().get("application/json").getSchema().get$ref());
        assertEquals("#/components/schemas/ErroResponse",
                respostas.get("503").getContent().get("application/json").getSchema().get$ref());
        assertNotNull(openApi.getComponents().getSchemas().get("ErroResponse"));
    }
}
