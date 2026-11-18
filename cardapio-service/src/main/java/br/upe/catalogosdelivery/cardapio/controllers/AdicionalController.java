package br.upe.catalogosdelivery.cardapio.controllers;

import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalPatchDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalRequestDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.AdicionalResponseDTO;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.DisponibilidadeRequestDTO;
import br.upe.catalogosdelivery.cardapio.services.AdicionalService;
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
@RequestMapping("/api/produtos/{produtoId}/adicionais")
@RequiredArgsConstructor
@Tag(name = "Adicionais", description = "Extras vinculados a um produto")
public class AdicionalController {
    private final AdicionalService adicionalService;

    @PostMapping
    @Operation(summary = "Criar adicional de um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adicional criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou cardápio excluído")
    })
    public ResponseEntity<AdicionalResponseDTO> criar(
            @PathVariable Long produtoId, @Valid @RequestBody AdicionalRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adicionalService.criar(produtoId, request));
    }

    @GetMapping
    @Operation(summary = "Listar adicionais de um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adicionais encontrados"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou cardápio excluído")
    })
    public ResponseEntity<List<AdicionalResponseDTO>> listar(@PathVariable Long produtoId) {
        return ResponseEntity.ok(adicionalService.listar(produtoId));
    }

    @PutMapping("/{adicionalId}")
    @Operation(summary = "Atualizar todos os dados de um adicional")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adicional atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto ou adicional inexistente")
    })
    public ResponseEntity<AdicionalResponseDTO> atualizar(
            @PathVariable Long produtoId, @PathVariable Long adicionalId,
            @Valid @RequestBody AdicionalRequestDTO request) {
        return ResponseEntity.ok(adicionalService.atualizar(produtoId, adicionalId, request));
    }

    @PatchMapping("/{adicionalId}")
    @Operation(summary = "Atualizar parcialmente um adicional")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adicional atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto ou adicional inexistente")
    })
    public ResponseEntity<AdicionalResponseDTO> atualizarParcialmente(
            @PathVariable Long produtoId, @PathVariable Long adicionalId,
            @Valid @RequestBody AdicionalPatchDTO request) {
        return ResponseEntity.ok(adicionalService.atualizarParcialmente(produtoId, adicionalId, request));
    }

    @PatchMapping("/{adicionalId}/disponibilidade")
    @Operation(summary = "Alterar disponibilidade de um adicional")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidade atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto ou adicional inexistente")
    })
    public ResponseEntity<AdicionalResponseDTO> alterarDisponibilidade(
            @PathVariable Long produtoId, @PathVariable Long adicionalId,
            @Valid @RequestBody DisponibilidadeRequestDTO request) {
        return ResponseEntity.ok(adicionalService.alterarDisponibilidade(
                produtoId, adicionalId, request.disponivel()));
    }

    @DeleteMapping("/{adicionalId}")
    @Operation(summary = "Excluir adicional")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Adicional excluído"),
            @ApiResponse(responseCode = "404", description = "Produto ou adicional inexistente")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long produtoId, @PathVariable Long adicionalId) {
        adicionalService.excluir(produtoId, adicionalId);
        return ResponseEntity.noContent().build();
    }
}
