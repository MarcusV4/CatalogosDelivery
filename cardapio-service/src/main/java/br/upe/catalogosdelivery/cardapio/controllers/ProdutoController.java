package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.services.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    @Autowired
    ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarTodosProdutos() {
        return ResponseEntity.ok(produtoService.listarProdutos());
    }

    @GetMapping("/{restauranteId}")
    public ResponseEntity<List<ProdutoResponseDTO>> listarProdutos(
            @PathVariable Long restauranteId) {

        return ResponseEntity.ok(
                produtoService.listarProdutosPorRestaurante(restauranteId)
        );

    }

    @PostMapping("/{restauranteId}")
    public ResponseEntity<ProdutoResponseDTO> cadastrarProduto(
            @RequestBody Produto produto,
            @PathVariable Long restauranteId) {

        ProdutoResponseDTO produtoDTO = produtoService.cadastrarProduto(
                produto, restauranteId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(produtoDTO);

    }

}
