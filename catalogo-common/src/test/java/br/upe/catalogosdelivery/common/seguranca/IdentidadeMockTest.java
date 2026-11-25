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

class IdentidadeMockTest {

    private static final Instant AGORA = Instant.parse("2026-11-25T12:00:00Z");
    private final IdentidadeMock mock = new IdentidadeMock(Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void leSubERoleDoPayloadSemExigirAssinatura() {
        assertEquals(new UsuarioAutenticado(1L, "LOJISTA"),
                mock.validarToken(jwt("{\"sub\":\"1\",\"role\":\"LOJISTA\"}")));
        assertEquals(new UsuarioAutenticado(57L, "USER"),
                mock.validarToken(jwt("{\"sub\":57,\"role\":\"USER\",\"exp\":" + AGORA.plusSeconds(60).getEpochSecond() + "}")));
    }

    @Test
    void recusaTokenQueNaoEhJwt() {
        for (String token : new String[]{"qualquer-coisa", "a.b", "a.%%%.c", ""}) {
            assertThrows(NaoAutenticadoException.class, () -> mock.validarToken(token), token);
        }
    }

    @Test
    void recusaSubAusenteOuNaoNumerico() {
        assertThrows(NaoAutenticadoException.class, () -> mock.validarToken(jwt("{\"role\":\"LOJISTA\"}")));
        assertThrows(NaoAutenticadoException.class, () -> mock.validarToken(jwt("{\"sub\":\"joao\"}")));
    }

    @Test
    void recusaTokenExpirado() {
        String vencido = jwt("{\"sub\":\"1\",\"exp\":" + AGORA.minusSeconds(1).getEpochSecond() + "}");
        assertEquals("Token inválido ou expirado.",
                assertThrows(NaoAutenticadoException.class, () -> mock.validarToken(vencido)).getMessage());
    }

    static String jwt(String payload) {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".";
    }
}
