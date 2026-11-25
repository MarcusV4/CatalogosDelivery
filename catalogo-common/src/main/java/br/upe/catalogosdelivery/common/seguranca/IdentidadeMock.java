package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * Mock da Identidade para o perfil {@code dev}, enquanto a Equipe 1 não entrega o serviço.
 * <p>
 * Lê as claims {@code sub} e {@code role} do JWT recebido <b>sem verificar a assinatura</b>
 * e recusa token malformado, sem {@code sub} numérico ou com {@code exp} vencido. Serve só
 * para desenvolvimento: qualquer um consegue montar um token aceito por ele.
 */
@Component
@Profile("dev")
public class IdentidadeMock implements IdentidadeClient {

    private static final Logger log = LoggerFactory.getLogger(IdentidadeMock.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Clock clock;

    public IdentidadeMock() {
        this(Clock.systemUTC());
    }

    IdentidadeMock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public UsuarioAutenticado validarToken(String token) {
        Claims claims = lerClaims(token);
        if (claims.sub() == null || !claims.sub().trim().matches("\\d+")) {
            return recusar("token sem sub numérico");
        }
        if (claims.exp() != null && !Instant.ofEpochSecond(claims.exp()).isAfter(clock.instant())) {
            return recusar("token expirado");
        }
        UsuarioAutenticado usuario = new UsuarioAutenticado(Long.valueOf(claims.sub().trim()), claims.role());
        log.info("[Identidade mock] token aceito: usuário {} ({})", usuario.id(), usuario.role());
        return usuario;
    }

    private Claims lerClaims(String token) {
        String[] partes = token == null ? new String[0] : token.split("\\.", -1);
        if (partes.length != 3) {
            return recusar("token não está no formato JWT");
        }
        try {
            return JSON.readValue(Base64.getUrlDecoder().decode(partes[1]), Claims.class);
        } catch (RuntimeException e) {
            return recusar("payload do token ilegível");
        }
    }

    private static <T> T recusar(String motivo) {
        log.info("[Identidade mock] token recusado: {}", motivo);
        throw new NaoAutenticadoException(IdentidadeHttpClient.MENSAGEM_TOKEN_INVALIDO);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Claims(String sub, String role, Long exp) {
    }
}
