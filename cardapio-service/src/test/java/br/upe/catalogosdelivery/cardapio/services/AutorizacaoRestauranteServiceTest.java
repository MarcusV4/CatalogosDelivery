package br.upe.catalogosdelivery.cardapio.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AutorizacaoRestauranteServiceTest {

    private final AutorizacaoRestauranteService service = new AutorizacaoRestauranteService();

    @Test
    void permiteValidarRestauranteEnquantoAutenticacaoAindaNaoFoiImplementada() {
        assertDoesNotThrow(() -> service.validarPermissao(10L));
    }
}
