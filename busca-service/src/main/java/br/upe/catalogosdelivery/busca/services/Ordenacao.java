package br.upe.catalogosdelivery.busca.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Converte o parâmetro {@code sort} ("campo" ou "campo,asc|desc") num {@link Comparator}.
 * Valores nulos ficam sempre no fim, em qualquer direção, e o desempate mantém a
 * paginação estável entre requisições.
 */
final class Ordenacao<T> {

    private final Map<String, Function<T, ? extends Comparable<?>>> campos = new LinkedHashMap<>();
    private final String campoPadrao;
    private final Comparator<T> desempate;

    Ordenacao(String campoPadrao, Comparator<T> desempate) {
        this.campoPadrao = campoPadrao;
        this.desempate = desempate;
    }

    Ordenacao<T> campo(String nome, Function<T, ? extends Comparable<?>> extrator) {
        campos.put(nome, extrator);
        return this;
    }

    Comparator<T> comparador(String sort) {
        String campo = campoPadrao;
        boolean decrescente = false;
        if (sort != null && !sort.isBlank()) {
            String[] partes = sort.split(",", -1);
            campo = partes[0].trim();
            if (partes.length > 2) {
                throw invalido(sort);
            }
            if (partes.length == 2) {
                String direcao = partes[1].trim().toLowerCase(Locale.ROOT);
                if (!direcao.equals("asc") && !direcao.equals("desc")) {
                    throw invalido(sort);
                }
                decrescente = direcao.equals("desc");
            }
        }
        Function<T, ? extends Comparable<?>> extrator = campos.get(campo);
        if (extrator == null) {
            throw invalido(sort);
        }
        return comparar(extrator, decrescente).thenComparing(desempate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Comparator<T> comparar(Function<T, ? extends Comparable<?>> extrator, boolean decrescente) {
        Comparator<Comparable> ordem = decrescente ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing((Function<T, Comparable>) (Function) extrator, Comparator.nullsLast(ordem));
    }

    private ResponseStatusException invalido(String sort) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordenação inválida: '" + sort
                + "'. Use sort=campo ou sort=campo,asc|desc, com campo entre " + campos.keySet() + ".");
    }
}
