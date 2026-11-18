package br.upe.catalogosdelivery.common.cors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigTest {

    private static final String FRONT_LOCAL = "http://localhost:5173";
    private static final String FRONT_CONTEINER = "http://localhost:3000";
    private static final String ORIGEM_ESTRANHA = "http://malicioso.exemplo";

    @RestController
    static class ControllerDeTeste {
        @GetMapping("/api/teste")
        String teste() {
            return "ok";
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({CorsConfig.class, ControllerDeTeste.class})
    static class ConfiguracaoDeTeste {
    }

    private static MockMvc mockMvc(WebApplicationContext contexto) {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Nested
    @SpringJUnitWebConfig(ConfiguracaoDeTeste.class)
    class ComOrigensPadrao {

        private MockMvc mockMvc;

        @BeforeEach
        void setUp(@Autowired WebApplicationContext contexto) {
            mockMvc = mockMvc(contexto);
        }

        @Test
        void preflightDeOrigemPermitidaLiberaMetodosECabecalhos() throws Exception {
            mockMvc.perform(options("/api/teste")
                            .header(HttpHeaders.ORIGIN, FRONT_LOCAL)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_LOCAL))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("DELETE")))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }

        @Test
        void preflightDoContainerDoFrontTambemEPermitido() throws Exception {
            mockMvc.perform(options("/api/teste")
                            .header(HttpHeaders.ORIGIN, FRONT_CONTEINER)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_CONTEINER));
        }

        @Test
        void preflightDeOrigemNaoPermitidaERecusado() throws Exception {
            mockMvc.perform(options("/api/teste")
                            .header(HttpHeaders.ORIGIN, ORIGEM_ESTRANHA)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        void respostaRealTrazOrigemPermitidaECabecalhoExposto() throws Exception {
            mockMvc.perform(get("/api/teste").header(HttpHeaders.ORIGIN, FRONT_LOCAL))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_LOCAL))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
        }
    }

    @Nested
    @SpringJUnitWebConfig(ConfiguracaoDeTeste.class)
    @TestPropertySource(properties = CorsConfig.PROPRIEDADE_ORIGENS + "= https://front.exemplo , http://localhost:4000")
    class ComOrigensConfiguradas {

        private MockMvc mockMvc;

        @BeforeEach
        void setUp(@Autowired WebApplicationContext contexto) {
            mockMvc = mockMvc(contexto);
        }

        @Test
        void aceitaAsOrigensDaPropriedadeIgnorandoEspacos() throws Exception {
            mockMvc.perform(options("/api/teste")
                            .header(HttpHeaders.ORIGIN, "https://front.exemplo")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://front.exemplo"));
            mockMvc.perform(options("/api/teste")
                            .header(HttpHeaders.ORIGIN, "http://localhost:4000")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isOk());
        }

        @Test
        void deixaDeAceitarAsOrigensPadrao() throws Exception {
            mockMvc.perform(options("/api/teste")
                            .header(HttpHeaders.ORIGIN, FRONT_LOCAL)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isForbidden());
        }
    }
}
