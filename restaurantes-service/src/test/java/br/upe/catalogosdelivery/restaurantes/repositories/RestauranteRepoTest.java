package br.upe.catalogosdelivery.restaurantes.repositories;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RestauranteRepoTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private RestauranteRepo restauranteRepo;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void persisteRestauranteEIdentificadorDoProprietarioNoPostgresIsolado() {
        Restaurante restaurante = new Restaurante();
        restaurante.setRestauranteNome("Pizzaria Recife");
        restaurante.setCidade("Recife");
        restaurante.setEstado("PE");
        restaurante.setOwnerUserId(7L);
        restaurante.setTaxaEntrega(550L);
        restaurante.setStatusRestaurante(StatusRestaurante.ABERTO);
        restaurante.setCategoriaRestaurante(CategoriaRestaurante.PIZZARIA);

        Restaurante salvo = restauranteRepo.saveAndFlush(restaurante);
        Restaurante encontrado = restauranteRepo.findById(salvo.getRestauranteId()).orElseThrow();

        assertTrue(salvo.getRestauranteId() > 0);
        assertEquals("Pizzaria Recife", encontrado.getRestauranteNome());
        assertEquals(7L, encontrado.getOwnerUserId());
        assertEquals(550L, encontrado.getTaxaEntrega());
    }
}
