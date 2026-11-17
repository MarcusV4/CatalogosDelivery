package br.upe.catalogosdelivery.busca;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Serviço de Busca. Não tem banco próprio: consulta os serviços de Restaurantes
 * e Cardápio via HTTP e aplica os filtros.
 */
@OpenAPIDefinition(info = @Info(
		title = "Catálogo - Busca",
		description = "Busca de restaurantes e itens do cardápio.",
		version = "1.0.0"))
@SpringBootApplication(scanBasePackages = {
		"br.upe.catalogosdelivery.busca",
		"br.upe.catalogosdelivery.common"
})
public class BuscaApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuscaApplication.class, args);
	}

}
