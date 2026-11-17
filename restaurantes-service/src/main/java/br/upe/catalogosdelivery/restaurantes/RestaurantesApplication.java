package br.upe.catalogosdelivery.restaurantes;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
		title = "Catálogo - Restaurantes",
		description = "Cadastro e consulta de restaurantes do Catálogo.",
		version = "1.0.0"))
@SpringBootApplication(scanBasePackages = {
		"br.upe.catalogosdelivery.restaurantes",
		"br.upe.catalogosdelivery.common"
})
public class RestaurantesApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaurantesApplication.class, args);
	}

}
