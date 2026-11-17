package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.services.CardapioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cardapios")
@RequiredArgsConstructor
@Tag(name = "Cardápios", description = "Integração entre Cardápio e Restaurantes")
public class CardapioController {

    private final CardapioService cardapioService;

    /** Usado pelo serviço de Restaurantes ao excluir um restaurante. */
    @DeleteMapping("/restaurante/{restauranteId}")
    @Operation(summary = "Excluir logicamente o cardápio de um restaurante", description = "Rota usada pelo serviço de Restaurantes durante a exclusão.")
    @ApiResponse(responseCode = "204", description = "Cardápio excluído ou inexistente")
    public ResponseEntity<Void> excluirCardapioDoRestaurante(@PathVariable Long restauranteId) {
        cardapioService.excluirCardapioDoRestaurante(restauranteId);
        return ResponseEntity.noContent().build();
    }
}
