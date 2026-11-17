package br.upe.catalogosdelivery.cardapio;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
		title = "Catálogo - Cardápio",
		description = "Cadastro, consulta e validação de produtos do Catálogo.",
		version = "1.0.0"))
@SpringBootApplication(scanBasePackages = {
		"br.upe.catalogosdelivery.cardapio",
		"br.upe.catalogosdelivery.common"
})
public class CardapioApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardapioApplication.class, args);
	}

}
