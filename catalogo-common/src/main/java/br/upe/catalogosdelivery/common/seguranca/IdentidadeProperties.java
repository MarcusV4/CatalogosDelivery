package br.upe.catalogosdelivery.common.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Configuração da integração com a Identidade (contrato OAuth da Equipe 1), lida de
 * {@code catalogo.identidade.*} ou das variáveis {@code CATALOGO_IDENTIDADE_*}.
 *
 * @param modo             {@code identidade} (padrão) valida os tokens de verdade; {@code mock} aceita
 *                         tokens sem assinatura e só serve para desenvolvimento local
 * @param emissor          valor exato esperado na claim {@code iss}
 * @param jwksUri          onde baixar as chaves públicas; vazio usa {@code <emissor>/.well-known/jwks.json}
 * @param audiencias       valores aceitos na claim {@code aud}; sem nenhum, as escritas respondem 503
 * @param rbacUrl          base do serviço de Autorização/RBAC, que expõe {@code POST /v1/tokens/validar}
 * @param validacaoOnline  consulta o RBAC a cada escrita (exigido pelo contrato para tokens de usuário)
 * @param timeout          limite das chamadas ao JWKS e ao RBAC
 * @param papeisLoja       papéis que podem gerenciar restaurantes e cardápios
 */
@ConfigurationProperties("catalogo.identidade")
public record IdentidadeProperties(
        @DefaultValue("identidade") Modo modo,
        @DefaultValue("http://127.0.0.1:18001") String emissor,
        String jwksUri,
        @DefaultValue List<String> audiencias,
        String rbacUrl,
        @DefaultValue("true") boolean validacaoOnline,
        @DefaultValue("3s") Duration timeout,
        @DefaultValue("LOJA") List<String> papeisLoja
) {

    public enum Modo { IDENTIDADE, MOCK }

    public IdentidadeProperties {
        emissor = semBarraFinal(emissor);
        audiencias = limpar(audiencias);
        papeisLoja = limpar(papeisLoja).stream().map(UsuarioAutenticado::normalizarPapel).toList();
    }

    public String jwksUriEfetiva() {
        return jwksUri == null || jwksUri.isBlank() ? emissor + "/.well-known/jwks.json" : jwksUri.trim();
    }

    /** Diz o que falta para validar tokens de verdade; vazio quando a configuração está completa. */
    public List<String> pendencias() {
        java.util.ArrayList<String> faltando = new java.util.ArrayList<>();
        if (emissor.isBlank()) faltando.add("catalogo.identidade.emissor");
        if (audiencias.isEmpty()) faltando.add("catalogo.identidade.audiencias");
        if (validacaoOnline && (rbacUrl == null || rbacUrl.isBlank())) faltando.add("catalogo.identidade.rbac-url");
        return faltando;
    }

    private static String semBarraFinal(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("/+$", "");
    }

    private static List<String> limpar(List<String> valores) {
        return valores == null ? List.of() : valores.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
    }

    @Override
    public String toString() {
        return "IdentidadeProperties[modo=" + modo.name().toLowerCase(Locale.ROOT) + ", emissor=" + emissor
                + ", audiencias=" + audiencias + ", rbacUrl=" + rbacUrl + ", validacaoOnline=" + validacaoOnline + "]";
    }
}
