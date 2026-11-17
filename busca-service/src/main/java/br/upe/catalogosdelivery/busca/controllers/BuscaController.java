package br.upe.catalogosdelivery.busca.controllers;

import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.busca.services.BuscaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Busca", description = "Filtros de restaurantes e itens")
public class BuscaController {

    private final BuscaService buscaService;

    @GetMapping("/restaurantes")
    @Operation(summary = "Buscar restaurantes", description = "Filtra por nome, categoria e status, ignorando maiúsculas e acentos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurantes encontrados"),
            @ApiResponse(responseCode = "503", description = "Serviço de Restaurantes indisponível")
    })
    public ResponseEntity<List<RestauranteDTO>> buscarRestaurantes(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(buscaService.buscarRestaurantes(nome, categoria, status));
    }

    @GetMapping("/itens")
    @Operation(summary = "Buscar itens", description = "Filtra por nome, categoria, restaurante e disponibilidade. Continua disponível sem os dados do restaurante se Restaurantes estiver fora do ar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itens encontrados"),
            @ApiResponse(responseCode = "503", description = "Serviço de Cardápio indisponível")
    })
    public ResponseEntity<List<ItemBuscaDTO>> buscarItens(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Long restauranteId,
            @RequestParam(required = false) Boolean disponivel) {
        return ResponseEntity.ok(buscaService.buscarItens(nome, categoria, restauranteId, disponivel));
    }
}
