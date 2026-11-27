package br.upe.catalogosdelivery.common.seguranca;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Usuário de um access token já validado.
 *
 * @param contaId claim {@code sub}: UUID da conta na Identidade ({@code conta_id}), em minúsculas.
 *                É o valor gravado em {@code ownerUserId}.
 * @param papel   papel atual da conta ({@code CLIENTE}, {@code LOJA}, {@code ENTREGADOR} ou {@code ADMIN}),
 *                normalizado: maiúsculas e sem o prefixo {@code ROLE_}
 */
public record UsuarioAutenticado(String contaId, String papel) {

    private static final Pattern UUID_CANONICO =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public UsuarioAutenticado {
        contaId = normalizarContaId(contaId);
        papel = normalizarPapel(papel);
    }

    public boolean possuiAlgumPapel(Collection<String> papeis) {
        return papeis.contains(papel);
    }

    /**
     * O {@code sub} de um token de usuário é o UUID da conta; qualquer outra coisa é recusada,
     * inclusive o {@code servico:<client_id>} dos tokens M2M. Só o formato canônico
     * (8-4-4-4-12): o {@code UUID.fromString} do Java aceitaria até {@code 1-1-1-1-1}.
     */
    public static String normalizarContaId(String sub) {
        if (sub == null || !UUID_CANONICO.matcher(sub.trim()).matches()) {
            throw new IllegalArgumentException("O sub do token não é o UUID de uma conta.");
        }
        return sub.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean ehContaIdValido(String sub) {
        try {
            normalizarContaId(sub);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String normalizarPapel(String papel) {
        if (papel == null || papel.isBlank()) {
            throw new IllegalArgumentException("Token sem papel (role).");
        }
        String maiusculo = papel.trim().toUpperCase(Locale.ROOT);
        return maiusculo.startsWith("ROLE_") ? maiusculo.substring("ROLE_".length()) : maiusculo;
    }
}
