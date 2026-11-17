package br.upe.catalogosdelivery.cardapio.migrations;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class CategoriaProdutoMigrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void limparSchema() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("drop schema public cascade");
            statement.execute("create schema public");
        }
    }

    @Test
    void converteOrdinaisExistentesParaNomesDasCategorias() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            // Mesma definição que o Hibernate gera para enum ordinal, incluindo o CHECK numérico
            statement.execute("create table produto (id bigint primary key, "
                    + "categoria smallint check (categoria between 0 and 8))");
            try (PreparedStatement insert = connection.prepareStatement(
                    "insert into produto (id, categoria) values (?, ?)")) {
                for (short ordinal = 0; ordinal <= 8; ordinal++) {
                    insert.setLong(1, ordinal + 1);
                    insert.setShort(2, ordinal);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }

        flyway().migrate();

        Map<Long, String> categorias = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select id, categoria from produto order by id")) {
            while (resultSet.next()) {
                categorias.put(resultSet.getLong("id"), resultSet.getString("categoria"));
            }
        }

        assertEquals(Map.of(
                1L, "LANCHE",
                2L, "PRATO_PRINCIPAL",
                3L, "PIZZA",
                4L, "ENTRADA",
                5L, "SOBREMESA",
                6L, "BEBIDA",
                7L, "VEGETARIANO",
                8L, "SALGADO",
                9L, "ORIENTAL"), categorias);
    }

    @Test
    void executaMigracaoEmBancoVazio() throws SQLException {
        flyway().migrate();

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select count(*) from flyway_schema_history where version = '1' and success")) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1));
        }
    }

    @Test
    void rejeitaOrdinalDesconhecidoSemAlterarODado() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("create table produto (id bigint primary key, categoria smallint)");
            statement.execute("insert into produto (id, categoria) values (1, 99)");
        }

        assertThrows(FlywayException.class, () -> flyway().migrate());

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select categoria from produto where id = 1")) {
            resultSet.next();
            assertEquals(99, resultSet.getInt("categoria"));
        }
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }
}
