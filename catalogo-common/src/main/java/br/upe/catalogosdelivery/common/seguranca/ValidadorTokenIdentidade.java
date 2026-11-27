package br.upe.catalogosdelivery.common.seguranca;

import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Validação de verdade, conforme o contrato da Identidade:
 * <ol>
 *   <li>local, pelo JWKS: assinatura e claims ({@link VerificadorJwtLocal});</li>
 *   <li>online, no RBAC: sessão ainda ativa e papel atual ({@link ValidacaoOnlineRbac}).</li>
 * </ol>
 * A etapa local vem primeiro para recusar tokens forjados ou vencidos sem chamar ninguém.
 * O papel devolvido é o do RBAC, que é a fonte autoritativa.
 */
public class ValidadorTokenIdentidade implements ValidadorDeTokens {

    private static final Logger log = LoggerFactory.getLogger(ValidadorTokenIdentidade.class);

    private final List<String> pendencias;
    private final VerificadorJwtLocal verificadorLocal;
    private final ValidacaoOnlineRbac validacaoOnline;

    ValidadorTokenIdentidade(List<String> pendencias, VerificadorJwtLocal verificadorLocal,
                             ValidacaoOnlineRbac validacaoOnline) {
        this.pendencias = pendencias;
        this.verificadorLocal = verificadorLocal;
        this.validacaoOnline = validacaoOnline;
    }

    public static ValidadorTokenIdentidade criar(IdentidadeProperties config, String origem) {
        List<String> pendencias = config.pendencias();
        if (!pendencias.isEmpty()) {
            // O serviço sobe (as leituras continuam públicas), mas toda escrita responde 503.
            log.error("[{}] Integração com a Identidade incompleta; escritas vão responder 503 até configurar: {}",
                    origem, pendencias);
            return new ValidadorTokenIdentidade(pendencias, null, null);
        }
        log.info("[{}] Validando tokens da Identidade: {}", origem, config);
        return new ValidadorTokenIdentidade(List.of(),
                VerificadorJwtLocal.comJwks(config, origem),
                config.validacaoOnline() ? ValidacaoOnlineRbac.criar(config, origem) : null);
    }

    @Override
    public UsuarioAutenticado validar(String accessToken) {
        if (!pendencias.isEmpty()) {
            throw new ServicoIndisponivelException(
                    "A validação de login não está configurada neste serviço (" + String.join(", ", pendencias) + ").",
                    null);
        }
        UsuarioAutenticado local = verificadorLocal.verificar(accessToken);
        if (validacaoOnline == null) {
            return local;
        }
        ValidacaoOnlineRbac.Resultado online = validacaoOnline.validar(accessToken);
        if (online.contaId() != null && !local.contaId().equalsIgnoreCase(online.contaId().trim())) {
            log.warn("RBAC validou outra conta para o mesmo token; requisição recusada");
            throw new NaoAutenticadoException("Token de acesso inválido. Entre novamente.");
        }
        return new UsuarioAutenticado(local.contaId(), online.papel());
    }
}
