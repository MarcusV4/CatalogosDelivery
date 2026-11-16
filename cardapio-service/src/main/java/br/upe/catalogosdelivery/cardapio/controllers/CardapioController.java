package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.services.CardapioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cardapios")
@RequiredArgsConstructor
public class CardapioController {

    private final CardapioService cardapioService;

    /** Usado pelo serviço de Restaurantes ao excluir um restaurante. */
    @DeleteMapping("/restaurante/{restauranteId}")
    public ResponseEntity<Void> excluirCardapioDoRestaurante(@PathVariable Long restauranteId) {
        cardapioService.excluirCardapioDoRestaurante(restauranteId);
        return ResponseEntity.noContent().build();
    }
}
