package br.upe.catalogosdelivery.common.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Escolhe o validador de tokens pelo {@code catalogo.identidade.modo}. */
@Configuration
@EnableConfigurationProperties(IdentidadeProperties.class)
public class IdentidadeConfig {

    @Bean
    ValidadorDeTokens validadorDeTokens(IdentidadeProperties config,
                                        @Value("${spring.application.name:catalogo}") String origem) {
        return switch (config.modo()) {
            case MOCK -> ValidadorTokenDev.criar(origem);
            case IDENTIDADE -> ValidadorTokenIdentidade.criar(config, origem);
        };
    }
}
