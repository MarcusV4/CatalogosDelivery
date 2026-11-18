package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias", description = "Categorias fixas de produtos")
public class CategoriaController {
    @GetMapping
    @Operation(summary = "Listar categorias de produtos")
    @ApiResponse(responseCode = "200", description = "Categorias disponíveis")
    public ResponseEntity<List<CategoriaProduto>> listar() {
        return ResponseEntity.ok(List.of(CategoriaProduto.values()));
    }
}
