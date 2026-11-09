package br.upe.catalogosdelivery.controllers;

import br.upe.catalogosdelivery.entities.Produto;
import br.upe.catalogosdelivery.services.ProdutoService;
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

    @GetMapping("/{restauranteId}")
    public ResponseEntity<List<Produto>> listarProdutos(
            @PathVariable Long restauranteId) {

        return ResponseEntity.ok(
                produtoService.listarProdutosPorRestaurante(restauranteId)
        );

    }

    @PostMapping("/{restauranteId}")
    public ResponseEntity<Produto> cadastrarProduto(
            @RequestBody Produto produto,
            @PathVariable Long restauranteId) {

        Produto novoProduto = produtoService.cadastrarProduto(
                produto, restauranteId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(novoProduto);

    }

}
