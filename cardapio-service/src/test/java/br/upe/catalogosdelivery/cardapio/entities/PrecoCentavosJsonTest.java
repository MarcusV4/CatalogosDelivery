package br.upe.catalogosdelivery.cardapio.entities;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
class PrecoCentavosJsonTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void serializaPrecoComoInteiroEmCentavos() throws Exception {
        String json = jsonMapper.writeValueAsString(
                new ProdutoResponseDTO(1L, "Pizza", null, 2590L, true, null, 10L));

        assertTrue(json.contains("\"preco\":2590"));
        Produto produto = jsonMapper.readValue("{\"preco\":2590,\"disponivel\":true}", Produto.class);
        assertEquals(2590L, produto.getPreco());
    }

    @Test
    void rejeitaPrecoDecimalEmVezDeTruncar() {
        assertThrows(MismatchedInputException.class,
                () -> jsonMapper.readValue("{\"preco\":25.90,\"disponivel\":true}", Produto.class));
    }
}
