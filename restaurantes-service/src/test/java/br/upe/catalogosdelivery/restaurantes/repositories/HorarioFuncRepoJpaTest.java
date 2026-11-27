package br.upe.catalogosdelivery.restaurantes.repositories;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.HorarioFuncionamento;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HorarioFuncRepoJpaTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private RestauranteRepo restauranteRepo;
    @Autowired private HorarioFuncRepo horarioFuncRepo;
    @Autowired private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void persisteCategoriasSemAcentosEConsultaEApagaHorariosDoRestaurante() {
        Restaurante restaurante = new Restaurante();
        restaurante.setRestauranteNome("Restaurante de teste");
        restaurante.setOwnerUserId("6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01");
        restaurante.setTaxaEntrega(0L);
        restaurante.setStatusRestaurante(StatusRestaurante.ABERTO);
        restaurante.setCategoriaRestaurante(CategoriaRestaurante.ACAI);
        restaurante = restauranteRepo.saveAndFlush(restaurante);

        HorarioFuncionamento horario = new HorarioFuncionamento();
        horario.setIdRestaurante(restaurante);
        horario.setDiaSemana(DiaSemana.SEGUNDA);
        horario.setHoraAbertura(LocalTime.of(9, 0));
        horario.setHoraFechamento(LocalTime.of(18, 0));
        horarioFuncRepo.saveAndFlush(horario);

        assertEquals("ACAI", jdbcTemplate.queryForObject("select categoria_restaurante from restaurante where restaurante_id = ?",
                String.class, restaurante.getRestauranteId()));
        assertEquals(1, horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(
                restaurante.getRestauranteId()).size());

        horarioFuncRepo.deleteAllByIdRestauranteRestauranteId(restaurante.getRestauranteId());
        assertEquals(0, horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(
                restaurante.getRestauranteId()).size());
    }
}
