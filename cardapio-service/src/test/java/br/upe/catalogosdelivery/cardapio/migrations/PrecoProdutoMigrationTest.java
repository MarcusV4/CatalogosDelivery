package br.upe.catalogosdelivery.cardapio.migrations;

import org.flywaydb.core.Flyway;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** V2: converte produto.preco de reais (numeric) para centavos (bigint). */
@Testcontainers
class PrecoProdutoMigrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void limparSchema() throws SQLException {
        executar("drop schema public cascade", "create schema public");
    }

    @Test
    void convertePrecosEmReaisParaCentavos() throws SQLException {
        // Mesma definição que o Hibernate gerava para o BigDecimal
        executar("create table produto (id bigint primary key, preco numeric(38,2))",
                "insert into produto (id, preco) values (1, 25.90), (2, 19.99), (3, 0), (4, null)");

        flyway().migrate();

        assertEquals("bigint", tipoDoPreco());
        Map<Long, Long> esperado = new HashMap<>();
        esperado.put(1L, 2590L);
        esperado.put(2L, 1999L);
        esperado.put(3L, 0L);
        esperado.put(4L, null);
        assertEquals(esperado, precos());
    }

    @Test
    void naoAlteraPrecosQueJaEstaoEmCentavos() throws SQLException {
        executar("create table produto (id bigint primary key, preco bigint)",
                "insert into produto (id, preco) values (1, 2590)");

        flyway().migrate();

        assertEquals(Map.of(1L, 2590L), precos());
    }

    @Test
    void executaEmBancoVazio() throws SQLException {
        flyway().migrate();

        try (Connection connection = conectar();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select count(*) from flyway_schema_history where version = '2' and success")) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1));
        }
    }

    @Test
    void criaAdicionalComChaveEstrangeiraEmBancoExistente() throws SQLException {
        executar("create table produto (id bigint primary key, preco bigint)");

        flyway().migrate();

        try (Connection connection = conectar();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select confdeltype from pg_constraint "
                             + "where conrelid = 'adicional'::regclass "
                             + "and conname = 'fk_adicional_produto'")) {
            assertEquals(true, resultSet.next());
            assertEquals("c", resultSet.getString(1)); // ON DELETE CASCADE
        }
    }

    private String tipoDoPreco() throws SQLException {
        try (Connection connection = conectar();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select data_type from information_schema.columns "
                     + "where table_name = 'produto' and column_name = 'preco'")) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private Map<Long, Long> precos() throws SQLException {
        Map<Long, Long> precos = new HashMap<>();
        try (Connection connection = conectar();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select id, preco from produto")) {
            while (resultSet.next()) {
                precos.put(resultSet.getLong("id"), resultSet.getObject("preco", Long.class));
            }
        }
        return precos;
    }

    private void executar(String... sqls) throws SQLException {
        try (Connection connection = conectar(); Statement statement = connection.createStatement()) {
            for (String sql : sqls) {
                statement.execute(sql);
            }
        }
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }
}
