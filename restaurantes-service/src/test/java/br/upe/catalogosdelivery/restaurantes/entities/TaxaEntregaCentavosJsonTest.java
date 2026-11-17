package br.upe.catalogosdelivery.restaurantes.entities;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JsonTest
class TaxaEntregaCentavosJsonTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void recebeTaxaComoInteiroERejeitaDecimal() throws Exception {
        RestauranteDTO restaurante = jsonMapper.readValue("{\"taxaEntrega\":550}", RestauranteDTO.class);
        assertEquals(550L, restaurante.taxaEntrega());

        assertThrows(MismatchedInputException.class,
                () -> jsonMapper.readValue("{\"taxaEntrega\":5.50}", RestauranteDTO.class));
    }
}
