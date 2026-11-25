package br.upe.catalogosdelivery.common.seguranca;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Libera uma rota de escrita (POST/PUT/PATCH/DELETE) da exigência de token.
 * Use só em operações que não alteram dados, como a validação de itens do Checkout.
 * As rotas GET já são públicas e não precisam desta anotação.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RotaPublica {
}
