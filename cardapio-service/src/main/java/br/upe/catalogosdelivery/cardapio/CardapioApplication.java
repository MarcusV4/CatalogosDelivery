package br.upe.catalogosdelivery.cardapio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"br.upe.catalogosdelivery.cardapio",
		"br.upe.catalogosdelivery.common"
})
public class CardapioApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardapioApplication.class, args);
	}

}
