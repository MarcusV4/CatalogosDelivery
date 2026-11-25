package br.upe.catalogosdelivery.restaurantes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RestauranteTimeConfig {
    @Bean
    Clock restauranteClock() {
        return Clock.systemUTC();
    }
}
