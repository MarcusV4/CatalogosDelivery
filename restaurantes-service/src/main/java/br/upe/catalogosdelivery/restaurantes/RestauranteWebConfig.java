package br.upe.catalogosdelivery.restaurantes;

import br.upe.catalogosdelivery.restaurantes.controllers.RestauranteExistenteInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra a verificação de restaurante existente nas rotas por id (404 antes da validação). */
@Configuration
public class RestauranteWebConfig implements WebMvcConfigurer {

    private final RestauranteExistenteInterceptor restauranteExistenteInterceptor;

    public RestauranteWebConfig(RestauranteExistenteInterceptor restauranteExistenteInterceptor) {
        this.restauranteExistenteInterceptor = restauranteExistenteInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restauranteExistenteInterceptor).addPathPatterns("/api/restaurantes/*/**");
    }
}
