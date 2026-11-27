package br.upe.catalogosdelivery.restaurantes.migrations;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** V3: pedido mínimo do restaurante, em centavos; nulo quando não há mínimo. */
@Testcontainers
class PedidoMinimoMigrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void limparSchema() throws SQLException {
        executar("drop schema public cascade", "create schema public");
    }

    @Test
    void bancoExistenteGanhaAColunaERestaurantesFicamSemMinimo() throws SQLException {
        executar("create table restaurante (restaurante_id bigint primary key, owner_user_id varchar(36) not null)",
                "insert into restaurante values (1, '6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01')");

        flyway().migrate();

        assertEquals("bigint", tipoDaColuna());
        assertNull(pedidoMinimo(1), "restaurantes antigos ficam sem pedido mínimo");
        executar("update restaurante set pedido_minimo = 2000 where restaurante_id = 1");
        assertEquals(2000L, pedidoMinimo(1));
    }

    @Test
    void bancoNovoSemATabelaNaoQuebraAMigracao() throws SQLException {
        flyway().migrate();

        assertNull(tipoDaColuna(), "a tabela é criada depois pelo Hibernate, já com a coluna");
    }

    @Test
    void colunaJaCriadaPeloHibernateFicaComoEsta() throws SQLException {
        executar("create table restaurante (restaurante_id bigint primary key, owner_user_id varchar(36) not null, "
                        + "pedido_minimo bigint)",
                "insert into restaurante values (1, '6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01', 1500)");

        flyway().migrate();

        assertEquals(1500L, pedidoMinimo(1));
    }

    private String tipoDaColuna() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement();
             ResultSet linhas = comando.executeQuery("select data_type from information_schema.columns "
                     + "where table_name = 'restaurante' and column_name = 'pedido_minimo'")) {
            return linhas.next() ? linhas.getString(1) : null;
        }
    }

    private Long pedidoMinimo(long id) throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement();
             ResultSet linhas = comando.executeQuery(
                     "select pedido_minimo from restaurante where restaurante_id = " + id)) {
            linhas.next();
            long valor = linhas.getLong(1);
            return linhas.wasNull() ? null : valor;
        }
    }

    private void executar(String... comandos) throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            for (String sql : comandos) {
                comando.execute(sql);
            }
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
