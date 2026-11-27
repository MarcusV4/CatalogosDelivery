package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.function.Consumer;

/**
 * Primeira etapa da validação: confere o access token localmente, sem chamar a Identidade a
 * cada requisição (as chaves públicas do JWKS ficam em cache e só são baixadas de novo quando
 * aparece um {@code kid} desconhecido).
 * <p>
 * Conforme o contrato: assinatura <b>RS256</b> com chave do JWKS, cabeçalho {@code typ: at+jwt},
 * {@code iss} igual ao emissor configurado, {@code aud} com uma das audiências aceitas,
 * {@code exp} no futuro, {@code sub} com o UUID da conta e {@code role} presente.
 * O papel lido aqui não é o definitivo: a segunda etapa consulta o papel atual no RBAC.
 */
public class VerificadorJwtLocal {

    private static final Logger log = LoggerFactory.getLogger(VerificadorJwtLocal.class);
    static final JOSEObjectType TIPO_ACCESS_TOKEN = new JOSEObjectType("at+jwt");
    private static final String CLAIM_PAPEL = "role";

    private final JwtDecoder decoder;
    private final String origem;

    VerificadorJwtLocal(JwtDecoder decoder, String origem) {
        this.decoder = decoder;
        this.origem = origem;
    }

    /** Baixa as chaves do JWKS publicado pela Identidade. */
    public static VerificadorJwtLocal comJwks(IdentidadeProperties config, String origem) {
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(config.timeout());
        http.setReadTimeout(config.timeout());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(config.jwksUriEfetiva())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(new RestTemplate(http))
                .jwtProcessorCustomizer(exigirTipoAccessToken())
                .build();
        decoder.setJwtValidator(validadores(config));
        return new VerificadorJwtLocal(decoder, origem);
    }

    /** Usa uma chave fixa no lugar do JWKS (testes). */
    static VerificadorJwtLocal comChavePublica(RSAPublicKey chave, IdentidadeProperties config) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(chave)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .jwtProcessorCustomizer(exigirTipoAccessToken())
                .build();
        decoder.setJwtValidator(validadores(config));
        return new VerificadorJwtLocal(decoder, "teste");
    }

    public UsuarioAutenticado verificar(String accessToken) {
        Jwt jwt;
        try {
            jwt = decoder.decode(accessToken);
        } catch (BadJwtException e) {
            // Assinatura, typ, iss, aud, exp, sub ou role inválidos: o problema é o token.
            log.info("[{}] token recusado na validação local: {}", origem, e.getMessage());
            throw new NaoAutenticadoException("Token de acesso inválido ou expirado. Entre novamente.", e);
        } catch (JwtException e) {
            // Não deu para baixar o JWKS: sem as chaves não há como validar, então a escrita é recusada.
            log.warn("[{}] xx não foi possível obter as chaves (JWKS) da Identidade: {}", origem, e.getMessage());
            throw new ServicoIndisponivelException(
                    "O serviço de autenticação está indisponível no momento. Tente novamente mais tarde.", e);
        }
        return new UsuarioAutenticado(jwt.getSubject(), jwt.getClaimAsString(CLAIM_PAPEL));
    }

    private static Consumer<ConfigurableJWTProcessor<SecurityContext>> exigirTipoAccessToken() {
        return processador -> processador.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(TIPO_ACCESS_TOKEN));
    }

    static OAuth2TokenValidator<Jwt> validadores(IdentidadeProperties config) {
        List<String> audiencias = config.audiencias();
        // Lista explícita: o padrão do Spring Security 7 inclui um validador que só aceita typ "JWT",
        // e o contrato da Identidade usa "at+jwt" (RFC 9068).
        JwtTypeValidator tipo = new JwtTypeValidator(TIPO_ACCESS_TOKEN.getType());
        tipo.setAllowEmpty(false);
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),             // exp/nbf, com 60 s de tolerância de relógio
                new JwtIssuerValidator(config.emissor()),
                tipo,
                exigir(jwt -> jwt.getExpiresAt() != null, "O token não tem exp."),
                exigir(jwt -> jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(audiencias::contains),
                        "O token não foi emitido para este serviço (aud)."),
                exigir(jwt -> UsuarioAutenticado.ehContaIdValido(jwt.getSubject()),
                        "O sub do token não é o UUID de uma conta."),
                exigir(jwt -> {
                    String papel = jwt.getClaimAsString(CLAIM_PAPEL);
                    return papel != null && !papel.isBlank();
                }, "O token não tem role."));
    }

    private static OAuth2TokenValidator<Jwt> exigir(java.util.function.Predicate<Jwt> regra, String motivo) {
        return jwt -> regra.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", motivo, null));
    }
}
