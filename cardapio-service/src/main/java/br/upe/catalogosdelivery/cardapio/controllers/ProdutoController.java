package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItensRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ValidacaoItemResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import br.upe.catalogosdelivery.cardapio.services.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos", description = "Cadastro, consulta e validação de produtos")
public class ProdutoController {

    @Autowired
    ProdutoService produtoService;

    @PostMapping("/validacao")
    @Operation(summary = "Validar itens para o Checkout", description = "Confere existência, disponibilidade e preço em centavos dos itens de um restaurante.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado da validação"),
            @ApiResponse(responseCode = "400", description = "Restaurante ou itens inválidos")
    })
    public ResponseEntity<List<ValidacaoItemResponseDTO>> validarItensCheckout(
            @RequestBody ValidacaoItensRequestDTO request) {
        return ResponseEntity.ok(produtoService.validarItens(request));
    }

    @GetMapping
    @Operation(summary = "Listar todos os produtos ativos")
    @ApiResponse(responseCode = "200", description = "Produtos encontrados")
    public ResponseEntity<List<ProdutoResponseDTO>> listarTodosProdutos() {
        return ResponseEntity.ok(produtoService.listarProdutos());
    }

    @GetMapping("/{restauranteId}")
    @Operation(summary = "Listar produtos de um restaurante")
    @ApiResponse(responseCode = "200", description = "Produtos encontrados")
    public ResponseEntity<List<ProdutoResponseDTO>> listarProdutos(
            @PathVariable Long restauranteId) {

        return ResponseEntity.ok(
                produtoService.listarProdutosPorRestaurante(restauranteId)
        );

    }

    @PostMapping("/{restauranteId}")
    @Operation(summary = "Cadastrar produto em um restaurante")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto cadastrado"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente"),
            @ApiResponse(responseCode = "503", description = "Serviço de Restaurantes indisponível")
    })
    public ResponseEntity<ProdutoResponseDTO> cadastrarProduto(
            @RequestBody Produto produto,
            @PathVariable Long restauranteId) {

        ProdutoResponseDTO produtoDTO = produtoService.cadastrarProduto(
                produto, restauranteId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(produtoDTO);

    }

}
