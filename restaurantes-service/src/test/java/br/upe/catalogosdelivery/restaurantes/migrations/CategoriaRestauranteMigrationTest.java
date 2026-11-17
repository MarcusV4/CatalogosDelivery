package br.upe.catalogosdelivery.restaurantes.migrations;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class CategoriaRestauranteMigrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void limparSchema() throws SQLException {
        executar("drop schema public cascade", "create schema public");
    }

    @Test
    void converteOrdinaisExistentesParaNomesSemAcento() throws SQLException {
        executar("create table restaurante (restaurante_id bigint primary key, categoria_restaurante smallint "
                        + "check (categoria_restaurante between 0 and 18))",
                "insert into restaurante (restaurante_id, categoria_restaurante) values "
                        + "(1, 8), (2, 12), (3, 17)");
        flyway().migrate();
        assertEquals(Map.of(1L, "COMIDA_ARABE", 2L, "COMIDA_SAUDAVEL", 3L, "ACAI"), categorias());
    }

    @Test
    void normalizaNomesComAcentosJaPersistidos() throws SQLException {
        executar("create table restaurante (restaurante_id bigint primary key, categoria_restaurante varchar(255))",
                "insert into restaurante (restaurante_id, categoria_restaurante) values "
                        + "(1, 'COMIDA_ÁRABE'), (2, 'COMIDA_SAUDÁVEL'), (3, 'AÇAÍ')");
        flyway().migrate();
        assertEquals(Map.of(1L, "COMIDA_ARABE", 2L, "COMIDA_SAUDAVEL", 3L, "ACAI"), categorias());
    }

    @Test
    void executaEmBancoVazio() throws SQLException {
        flyway().migrate();
        try (Connection connection = conectar();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select count(*) from flyway_schema_history where version = '1' and success")) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1));
        }
    }

    @Test
    void rejeitaOrdinalSemMapeamento() throws SQLException {
        executar("create table restaurante (restaurante_id bigint primary key, categoria_restaurante smallint)",
                "insert into restaurante (restaurante_id, categoria_restaurante) values (1, 99)");
        assertThrows(FlywayException.class, () -> flyway().migrate());
    }

    private Map<Long, String> categorias() throws SQLException {
        try (Connection connection = conectar();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select restaurante_id, categoria_restaurante from restaurante order by restaurante_id")) {
            java.util.HashMap<Long, String> result = new java.util.HashMap<>();
            while (resultSet.next()) {
                result.put(resultSet.getLong(1), resultSet.getString(2));
            }
            return result;
        }
    }

    private void executar(String... comandos) throws SQLException {
        try (Connection connection = conectar(); Statement statement = connection.createStatement()) {
            for (String comando : comandos) statement.execute(comando);
        }
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private Flyway flyway() {
        return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .baselineOnMigrate(true).baselineVersion("0").load();
    }
}
