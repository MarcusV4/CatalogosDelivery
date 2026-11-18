package br.upe.catalogosdelivery.common.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Libera o front-end a chamar as rotas {@code /api/**} dos serviços pelo navegador.
 * As origens vêm de {@code catalogo.cors.origens-permitidas} (lista separada por vírgula,
 * ou a variável de ambiente {@code CATALOGO_CORS_ORIGENS_PERMITIDAS}); o padrão cobre o
 * front local ({@code npm run dev}) e o contêiner do Compose.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    static final String PROPRIEDADE_ORIGENS = "catalogo.cors.origens-permitidas";
    static final String ORIGENS_PADRAO = "http://localhost:5173,http://localhost:3000";

    private static final String ROTAS_DA_API = "/api/**";
    private static final long CACHE_PREFLIGHT_SEGUNDOS = 3600;

    private final String[] origensPermitidas;

    public CorsConfig(@Value("${" + PROPRIEDADE_ORIGENS + ":" + ORIGENS_PADRAO + "}") String origens) {
        this.origensPermitidas = Arrays.stream(origens.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(ROTAS_DA_API)
                .allowedOrigins(origensPermitidas)
                .allowedMethods(
                        HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name())
                .allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION)
                .exposedHeaders(HttpHeaders.LOCATION)
                .allowCredentials(false)
                .maxAge(CACHE_PREFLIGHT_SEGUNDOS);
    }
}
