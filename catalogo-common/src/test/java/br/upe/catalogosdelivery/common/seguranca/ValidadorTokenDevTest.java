package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorTokenDevTest {

    private final ValidadorTokenDev validador =
            new ValidadorTokenDev(Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC), "teste");

    @Test
    void aceitaTokenSemAssinaturaComContaEPapel() {
        UsuarioAutenticado usuario = validador.validar(semAssinatura(
                "{\"sub\":\"" + TokensDeTeste.CONTA + "\",\"role\":\"LOJA\"}"));

        assertEquals(TokensDeTeste.CONTA, usuario.contaId());
        assertEquals("LOJA", usuario.papel());
    }

    @Test
    void recusaTokenVencidoSubQueNaoEhUuidOuSemPapel() {
        assertThrows(NaoAutenticadoException.class, () -> validador.validar(semAssinatura(
                "{\"sub\":\"" + TokensDeTeste.CONTA + "\",\"role\":\"LOJA\",\"exp\":1700000000}")));
        assertThrows(NaoAutenticadoException.class,
                () -> validador.validar(semAssinatura("{\"sub\":\"1\",\"role\":\"LOJA\"}")));
        assertThrows(NaoAutenticadoException.class,
                () -> validador.validar(semAssinatura("{\"sub\":\"" + TokensDeTeste.CONTA + "\"}")));
        assertThrows(NaoAutenticadoException.class, () -> validador.validar("lixo"));
    }

    static String semAssinatura(String payload) {
        Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
        return base64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + base64.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".";
    }
}
