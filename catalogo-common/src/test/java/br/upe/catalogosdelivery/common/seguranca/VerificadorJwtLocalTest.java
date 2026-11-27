package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static br.upe.catalogosdelivery.common.seguranca.TokensDeTeste.CONTA;
import static br.upe.catalogosdelivery.common.seguranca.TokensDeTeste.config;
import static br.upe.catalogosdelivery.common.seguranca.TokensDeTeste.novaChave;
import static br.upe.catalogosdelivery.common.seguranca.TokensDeTeste.token;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificadorJwtLocalTest {

    private final RSAKey chave = novaChave();
    private final VerificadorJwtLocal verificador =
            VerificadorJwtLocal.comChavePublica(publica(chave), config(null, null));
    private HttpServer servidor;

    @AfterEach
    void pararServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void aceitaTokenDoContratoELeContaEPapel() {
        UsuarioAutenticado usuario = verificador.verificar(token(chave, c -> c.claim("role", "loja")));

        assertEquals(CONTA, usuario.contaId());
        assertEquals("LOJA", usuario.papel());
    }

    @Test
    void recusaTokenAssinadoPorOutraChave() {
        assertRecusado(token(novaChave(), c -> { }));
    }

    @Test
    void recusaTokenSemOTipoAtJwt() {
        assertRecusado(token(chave, JOSEObjectType.JWT, c -> { }));
    }

    @Test
    void recusaTokenDeOutroEmissor() {
        assertRecusado(token(chave, c -> c.issuer("http://outro-emissor")));
    }

    @Test
    void recusaTokenEmitidoParaOutroServico() {
        assertRecusado(token(chave, c -> c.audience("http://perfis:8002")));
    }

    @Test
    void recusaTokenVencido() {
        assertRecusado(token(chave, c -> c.expirationTime(Date.from(Instant.now().minusSeconds(120)))));
    }

    @Test
    void recusaTokenSemExp() {
        assertRecusado(token(chave, c -> c.expirationTime(null)));
    }

    @Test
    void recusaTokenDeServicoM2MPorqueOSubNaoEhUmaConta() {
        assertRecusado(token(chave, c -> c.subject("servico:worker-notificacoes-financeiro").claim("role", null)));
        assertRecusado(token(chave, c -> c.subject("servico:worker-notificacoes-financeiro")));
    }

    @Test
    void recusaTokenSemPapel() {
        assertRecusado(token(chave, c -> c.claim("role", null)));
    }

    @Test
    void recusaTextoQueNaoEhJwt() {
        assertRecusado("nao-e-um-jwt");
    }

    @Test
    void validaPeloJwksPublicadoPelaIdentidade() throws Exception {
        String jwks = new JWKSet(chave.toPublicJWK()).toString();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/.well-known/jwks.json", troca -> {
            byte[] corpo = jwks.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(200, corpo.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        });
        servidor.start();
        String uri = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/.well-known/jwks.json";

        VerificadorJwtLocal peloJwks = VerificadorJwtLocal.comJwks(config(uri, null), "teste");

        assertEquals(CONTA, peloJwks.verificar(token(chave, c -> { })).contaId());
        assertRecusadoPor(peloJwks, token(novaChave(), c -> { }));
    }

    @Test
    void respondeIndisponivelQuandoNaoConsegueBaixarOJwks() {
        VerificadorJwtLocal semJwks =
                VerificadorJwtLocal.comJwks(config("http://127.0.0.1:1/.well-known/jwks.json", null), "teste");

        assertThrows(ServicoIndisponivelException.class, () -> semJwks.verificar(token(chave, c -> { })));
    }

    private void assertRecusado(String token) {
        assertRecusadoPor(verificador, token);
    }

    private static void assertRecusadoPor(VerificadorJwtLocal verificador, String token) {
        assertThrows(NaoAutenticadoException.class, () -> verificador.verificar(token));
    }

    private static java.security.interfaces.RSAPublicKey publica(RSAKey chave) {
        try {
            return chave.toRSAPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
