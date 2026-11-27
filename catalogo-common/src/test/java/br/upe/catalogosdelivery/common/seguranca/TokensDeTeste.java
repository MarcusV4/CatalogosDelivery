package br.upe.catalogosdelivery.common.seguranca;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Gera chaves RSA e access tokens no formato do contrato da Identidade, para os testes. */
final class TokensDeTeste {

    static final String EMISSOR = "http://127.0.0.1:18001";
    static final String AUDIENCIA = "catalogo";
    static final String CONTA = "6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01";

    private TokensDeTeste() {
    }

    static RSAKey novaChave() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).keyUse(KeyUse.SIGNATURE).generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static IdentidadeProperties config(String jwksUri, String rbacUrl) {
        return new IdentidadeProperties(IdentidadeProperties.Modo.IDENTIDADE, EMISSOR, jwksUri, List.of(AUDIENCIA),
                rbacUrl, rbacUrl != null, Duration.ofSeconds(2), List.of("LOJA"));
    }

    /** Token válido; {@code ajuste} altera as claims para montar os casos de erro. */
    static String token(RSAKey chave, Consumer<JWTClaimsSet.Builder> ajuste) {
        return token(chave, new JOSEObjectType("at+jwt"), ajuste);
    }

    static String token(RSAKey chave, JOSEObjectType tipo, Consumer<JWTClaimsSet.Builder> ajuste) {
        Instant agora = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(EMISSOR)
                .subject(CONTA)
                .audience(AUDIENCIA)
                .issueTime(Date.from(agora))
                .expirationTime(Date.from(agora.plus(Duration.ofMinutes(25))))
                .jwtID(UUID.randomUUID().toString())
                .claim("client_id", "catalogo-web")
                .claim("role", "LOJA");
        ajuste.accept(claims);
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(chave.getKeyID()).type(tipo).build(),
                    claims.build());
            jwt.sign(new RSASSASigner(chave));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
