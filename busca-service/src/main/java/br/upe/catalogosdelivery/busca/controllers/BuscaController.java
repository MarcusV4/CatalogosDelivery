package br.upe.catalogosdelivery.busca.controllers;

import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.busca.services.BuscaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/busca")
@RequiredArgsConstructor
public class BuscaController {

    private final BuscaService buscaService;

    @GetMapping("/restaurantes")
    public ResponseEntity<List<RestauranteDTO>> buscarRestaurantes(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(buscaService.buscarRestaurantes(nome, categoria, status));
    }

    @GetMapping("/itens")
    public ResponseEntity<List<ItemBuscaDTO>> buscarItens(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Long restauranteId,
            @RequestParam(required = false) Boolean disponivel) {
        return ResponseEntity.ok(buscaService.buscarItens(nome, categoria, restauranteId, disponivel));
    }
}
