package br.upe.catalogosdelivery.common.seguranca;

import java.util.Collection;
import java.util.Locale;

/**
 * Usuário identificado pelo token validado na Identidade (Equipe 1).
 *
 * @param id   claim {@code sub} do JWT; é o valor gravado em {@code ownerUserId}
 * @param role claim {@code role} do JWT (perfil do RBAC da Identidade)
 */
public record UsuarioAutenticado(Long id, String role) {

    /** Compara ignorando maiúsculas e o prefixo {@code ROLE_} do Spring, se a Identidade usar. */
    public boolean possuiAlgumaRole(Collection<String> roles) {
        if (role == null) {
            return false;
        }
        String normalizada = normalizar(role);
        return roles.stream().map(UsuarioAutenticado::normalizar).anyMatch(normalizada::equals);
    }

    private static String normalizar(String role) {
        String maiuscula = role.trim().toUpperCase(Locale.ROOT);
        return maiuscula.startsWith("ROLE_") ? maiuscula.substring(5) : maiuscula;
    }
}
