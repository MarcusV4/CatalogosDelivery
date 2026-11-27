package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Date;

/**
 * Modo {@code mock}, <b>só para desenvolvimento local</b> sem os serviços da Identidade no ar:
 * lê {@code sub} e {@code role} do JWT <b>sem verificar a assinatura</b> e confere só o formato
 * do {@code sub} e o {@code exp}, se houver. Qualquer pessoa consegue montar um token aceito
 * aqui, por isso o modo é desligado por padrão e avisa no log ao subir.
 */
public class ValidadorTokenDev implements ValidadorDeTokens {

    private static final Logger log = LoggerFactory.getLogger(ValidadorTokenDev.class);

    private final Clock clock;

    ValidadorTokenDev(Clock clock, String origem) {
        this.clock = clock;
        log.warn("[{}] ATENÇÃO: catalogo.identidade.modo=mock. Tokens são aceitos SEM verificar a assinatura; "
                + "use só em desenvolvimento local.", origem);
    }

    public static ValidadorTokenDev criar(String origem) {
        return new ValidadorTokenDev(Clock.systemUTC(), origem);
    }

    @Override
    public UsuarioAutenticado validar(String accessToken) {
        try {
            JWT jwt = JWTParser.parse(accessToken);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp != null && !exp.toInstant().isAfter(clock.instant())) {
                throw new NaoAutenticadoException("Token de acesso expirado. Entre novamente.");
            }
            return new UsuarioAutenticado(claims.getSubject(), claims.getStringClaim("role"));
        } catch (NaoAutenticadoException e) {
            throw e;
        } catch (Exception e) {
            log.info("Token de dev recusado: {}", e.getMessage());
            throw new NaoAutenticadoException("Token de acesso inválido. Entre novamente.", e);
        }
    }
}
