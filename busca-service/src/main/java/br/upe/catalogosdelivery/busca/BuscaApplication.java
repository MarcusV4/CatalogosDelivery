package br.upe.catalogosdelivery.busca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Serviço de Busca. Não tem banco próprio: consulta os serviços de Restaurantes
 * e Cardápio via HTTP e aplica os filtros.
 */
@SpringBootApplication(scanBasePackages = {
		"br.upe.catalogosdelivery.busca",
		"br.upe.catalogosdelivery.common"
})
public class BuscaApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuscaApplication.class, args);
	}

}
