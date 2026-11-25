package br.upe.catalogosdelivery.common.openapi;

import br.upe.catalogosdelivery.common.seguranca.RotaPublica;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenApiSegurancaConfigTest {

    private final OpenApiSegurancaConfig config = new OpenApiSegurancaConfig();

    @Test
    void registraOEsquemaBearer() {
        OpenAPI openApi = new OpenAPI();
        config.esquemaBearer().customise(openApi);

        SecurityScheme esquema = openApi.getComponents().getSecuritySchemes().get("bearerAuth");
        assertEquals(SecurityScheme.Type.HTTP, esquema.getType());
        assertEquals("bearer", esquema.getScheme());
        assertEquals("JWT", esquema.getBearerFormat());
    }

    @Test
    void escritaGanhaCadeadoE401403503SemSobrescreverAsDescricoesDoController() throws Exception {
        Operation operacao = new Operation().responses(new ApiResponses()
                .addApiResponse("403", new ApiResponse().description("Usuário não é proprietário")));

        config.rotasProtegidas().customize(operacao, metodo("escrever"));

        assertEquals("bearerAuth", operacao.getSecurity().get(0).keySet().iterator().next());
        assertEquals("Token ausente ou inválido", operacao.getResponses().get("401").getDescription());
        assertEquals("Usuário não é proprietário", operacao.getResponses().get("403").getDescription());
        assertEquals("Serviço de identidade indisponível", operacao.getResponses().get("503").getDescription());
    }

    @Test
    void leituraERotaPublicaFicamSemCadeado() throws Exception {
        for (String nome : new String[]{"ler", "publica"}) {
            Operation operacao = new Operation();
            config.rotasProtegidas().customize(operacao, metodo(nome));
            assertNull(operacao.getSecurity(), nome);
            assertNull(operacao.getResponses(), nome);
        }
    }

    private static HandlerMethod metodo(String nome) throws NoSuchMethodException {
        return new HandlerMethod(new Controller(), Controller.class.getDeclaredMethod(nome));
    }

    static class Controller {
        @GetMapping("/x")
        void ler() {
        }

        @PatchMapping("/x")
        void escrever() {
        }

        @RotaPublica
        @PostMapping("/x/publica")
        void publica() {
        }
    }
}
