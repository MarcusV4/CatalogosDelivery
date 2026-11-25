package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.services.CardapioService;
import br.upe.catalogosdelivery.cardapio.services.ProdutoService;
import br.upe.catalogosdelivery.cardapio.mappers.ProdutoMapper;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ProdutoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Cardápios", description = "Integração entre Cardápio e Restaurantes")
public class CardapioController {

    private final CardapioService cardapioService;
    private final ProdutoService produtoService;
    private final ProdutoMapper produtoMapper;

    @GetMapping("/restaurantes/{restauranteId}/cardapio")
    @Operation(summary = "Consultar o cardápio de um restaurante")
    @ApiResponse(responseCode = "200", description = "Produtos do cardápio; lista vazia se não houver itens")
    public ResponseEntity<List<ProdutoResponseDTO>> listarProdutos(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(produtoService.listarProdutosPorRestaurante(restauranteId));
    }

    @PostMapping("/restaurantes/{restauranteId}/cardapio")
    @Operation(summary = "Cadastrar produto no cardápio de um restaurante")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente ou cardápio excluído"),
            @ApiResponse(responseCode = "409", description = "Restaurante desativado (INDISPONIVEL)"),
            @ApiResponse(responseCode = "503", description = "Serviço de Restaurantes ou de Identidade indisponível")
    })
    public ResponseEntity<ProdutoResponseDTO> cadastrarProduto(
            @PathVariable Long restauranteId, @Valid @RequestBody ProdutoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(produtoService.cadastrarProduto(produtoMapper.toEntity(request), restauranteId));
    }

    /**
     * Rota administrativa: o serviço de Restaurantes não a chama mais, porque o DELETE de
     * restaurante é uma exclusão lógica (status INDISPONIVEL) e o restaurante pode ser reativado.
     */
    @DeleteMapping("/cardapios/restaurante/{restauranteId}")
    @Operation(summary = "Excluir logicamente o cardápio de um restaurante", description = "Rota administrativa, restrita ao dono do restaurante. Não é chamada pelo serviço de Restaurantes: a exclusão de restaurante é lógica e preserva o cardápio.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cardápio excluído ou inexistente"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário do restaurante"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")
    })
    public ResponseEntity<Void> excluirCardapioDoRestaurante(@PathVariable Long restauranteId) {
        cardapioService.excluirCardapioDoRestaurante(restauranteId);
        return ResponseEntity.noContent().build();
    }
}
