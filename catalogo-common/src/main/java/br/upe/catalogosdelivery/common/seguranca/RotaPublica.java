package br.upe.catalogosdelivery.common.seguranca;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Libera uma rota de escrita do token de usuário. Leituras (GET) já são públicas;
 * use só em escritas que não pertencem a um dono, como a validação de itens do Checkout.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RotaPublica {
}
