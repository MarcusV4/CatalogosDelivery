package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static br.upe.catalogosdelivery.common.seguranca.TokensDeTeste.CONTA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidadorTokenIdentidadeTest {

    private final RSAKey chave = TokensDeTeste.novaChave();
    private final VerificadorJwtLocal local = verificador();
    private final ValidacaoOnlineRbac online = mock(ValidacaoOnlineRbac.class);
    private final ValidadorTokenIdentidade validador = new ValidadorTokenIdentidade(List.of(), local, online);

    @Test
    void usaOPapelAtualDoRbacNoLugarDoPapelDoToken() {
        String token = TokensDeTeste.token(chave, c -> c.claim("role", "LOJA"));
        when(online.validar(token)).thenReturn(new ValidacaoOnlineRbac.Resultado(CONTA, "CLIENTE"));

        UsuarioAutenticado usuario = validador.validar(token);

        assertEquals(CONTA, usuario.contaId());
        assertEquals("CLIENTE", usuario.papel());
    }

    @Test
    void naoConsultaORbacQuandoOTokenJaFalhaNaValidacaoLocal() {
        String forjado = TokensDeTeste.token(TokensDeTeste.novaChave(), c -> { });

        assertThrows(NaoAutenticadoException.class, () -> validador.validar(forjado));
        verify(online, never()).validar(anyString());
    }

    @Test
    void recusaQuandoORbacValidaOutraConta() {
        String token = TokensDeTeste.token(chave, c -> { });
        when(online.validar(token)).thenReturn(
                new ValidacaoOnlineRbac.Resultado("00000000-0000-0000-0000-000000000009", "LOJA"));

        assertThrows(NaoAutenticadoException.class, () -> validador.validar(token));
    }

    @Test
    void semValidacaoOnlineUsaSoAValidacaoLocal() {
        ValidadorTokenIdentidade soLocal = new ValidadorTokenIdentidade(List.of(), local, null);

        assertEquals("LOJA", soLocal.validar(TokensDeTeste.token(chave, c -> { })).papel());
    }

    @Test
    void configuracaoIncompletaRecusaAsEscritasComIndisponivel() {
        IdentidadeProperties incompleta = new IdentidadeProperties(IdentidadeProperties.Modo.IDENTIDADE,
                "http://127.0.0.1:18001", null, List.of(), null, true, Duration.ofSeconds(1), List.of("LOJA"));
        assertTrue(incompleta.pendencias().contains("catalogo.identidade.audiencias"));
        assertTrue(incompleta.pendencias().contains("catalogo.identidade.rbac-url"));

        ValidadorTokenIdentidade semConfig = ValidadorTokenIdentidade.criar(incompleta, "teste");

        ServicoIndisponivelException erro =
                assertThrows(ServicoIndisponivelException.class, () -> semConfig.validar("qualquer"));
        assertTrue(erro.getMessage().contains("catalogo.identidade.audiencias"));
    }

    private VerificadorJwtLocal verificador() {
        try {
            return VerificadorJwtLocal.comChavePublica(chave.toRSAPublicKey(), TokensDeTeste.config(null, null));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
