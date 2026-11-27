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

/** V2: o dono do restaurante passa de número para o UUID da conta da Identidade. */
@Testcontainers
class OwnerUserIdMigrationTest {

    private static final String CONTA = "6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01";

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @BeforeEach
    void limparSchema() throws SQLException {
        executar("drop schema public cascade", "create schema public");
    }

    @Test
    void converteDonoNumericoDeBancoExistenteParaTexto() throws SQLException {
        executar("create table restaurante (restaurante_id bigint primary key, owner_user_id bigint not null)",
                "insert into restaurante values (1, 7)");

        flyway().migrate();

        assertEquals("character varying", tipoDaColuna());
        assertEquals("7", donoDoRestaurante(1));
        executar("update restaurante set owner_user_id = '" + CONTA + "' where restaurante_id = 1");
        assertEquals(CONTA, donoDoRestaurante(1));
    }

    @Test
    void bancoNovoSemATabelaNaoQuebraAMigracao() throws SQLException {
        flyway().migrate();

        assertNull(tipoDaColuna(), "a tabela é criada depois pelo Hibernate, já com varchar");
    }

    @Test
    void colunaJaEmTextoFicaComoEsta() throws SQLException {
        executar("create table restaurante (restaurante_id bigint primary key, owner_user_id varchar(36) not null)",
                "insert into restaurante values (1, '" + CONTA + "')");

        flyway().migrate();

        assertEquals(CONTA, donoDoRestaurante(1));
    }

    private String tipoDaColuna() throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement();
             ResultSet linhas = comando.executeQuery("select data_type from information_schema.columns "
                     + "where table_name = 'restaurante' and column_name = 'owner_user_id'")) {
            return linhas.next() ? linhas.getString(1) : null;
        }
    }

    private String donoDoRestaurante(long id) throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement();
             ResultSet linhas = comando.executeQuery(
                     "select owner_user_id from restaurante where restaurante_id = " + id)) {
            linhas.next();
            return linhas.getString(1);
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
