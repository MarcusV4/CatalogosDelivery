package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.*;
import br.upe.catalogosdelivery.cardapio.mappers.ProdutoMapper;
import br.upe.catalogosdelivery.cardapio.services.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Cadastro, consulta e validação de produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final ProdutoMapper produtoMapper;

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

    @GetMapping("/{id}")
    @Operation(summary = "Consultar produto por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou cardápio excluído")
    })
    public ResponseEntity<ProdutoResponseDTO> buscarProduto(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarProduto(id));
    }

    @Deprecated
    @PostMapping("/{restauranteId}")
    @Operation(summary = "Cadastrar produto em um restaurante (rota antiga)",
            description = "Use POST /api/restaurantes/{restauranteId}/cardapio.", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente ou cardápio excluído"),
            @ApiResponse(responseCode = "503", description = "Serviço de Restaurantes indisponível")
    })
    public ResponseEntity<ProdutoResponseDTO> cadastrarProduto(
            @Valid @RequestBody ProdutoRequestDTO request, @PathVariable Long restauranteId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(produtoService.cadastrarProduto(produtoMapper.toEntity(request), restauranteId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar todos os dados de um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou cardápio excluído")
    })
    public ResponseEntity<ProdutoResponseDTO> atualizarProduto(
            @PathVariable Long id, @Valid @RequestBody ProdutoRequestDTO request) {
        return ResponseEntity.ok(produtoService.atualizarProduto(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar parcialmente um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou cardápio excluído")
    })
    public ResponseEntity<ProdutoResponseDTO> atualizarParcialmenteProduto(
            @PathVariable Long id, @Valid @RequestBody ProdutoPatchDTO request) {
        return ResponseEntity.ok(produtoService.atualizarParcialmenteProduto(id, request));
    }

    @PatchMapping("/{id}/disponibilidade")
    @Operation(summary = "Alterar disponibilidade de um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidade atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou cardápio excluído")
    })
    public ResponseEntity<ProdutoResponseDTO> alterarDisponibilidade(
            @PathVariable Long id, @Valid @RequestBody DisponibilidadeRequestDTO request) {
        return ResponseEntity.ok(produtoService.alterarDisponibilidade(id, request.disponivel()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir produto e seus adicionais")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto excluído"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou cardápio excluído")
    })
    public ResponseEntity<Void> excluirProduto(@PathVariable Long id) {
        produtoService.excluirProduto(id);
        return ResponseEntity.noContent().build();
    }
}
