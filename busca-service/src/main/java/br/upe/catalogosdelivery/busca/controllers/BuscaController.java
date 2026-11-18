package br.upe.catalogosdelivery.busca.controllers;

import br.upe.catalogosdelivery.busca.dto.FiltroItens;
import br.upe.catalogosdelivery.busca.dto.FiltroRestaurantes;
import br.upe.catalogosdelivery.busca.dto.ItemBuscaDTO;
import br.upe.catalogosdelivery.busca.dto.PaginaDTO;
import br.upe.catalogosdelivery.busca.dto.RestauranteDTO;
import br.upe.catalogosdelivery.busca.services.BuscaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/busca")
@RequiredArgsConstructor
@Tag(name = "Busca", description = "Filtros de restaurantes e itens (\"Buscar Prato\")")
public class BuscaController {

    private final BuscaService buscaService;

    @GetMapping("/restaurantes")
    @Operation(summary = "Buscar restaurantes",
            description = "Filtros combináveis; textos ignoram maiúsculas e acentos. Restaurantes desativados "
                    + "não aparecem. Resultado paginado e ordenado (padrão: nome crescente).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de restaurantes encontrados"),
            @ApiResponse(responseCode = "400", description = "Paginação ou ordenação inválida"),
            @ApiResponse(responseCode = "503", description = "Serviço de Restaurantes indisponível")
    })
    public ResponseEntity<PaginaDTO<RestauranteDTO>> buscarRestaurantes(
            @Parameter(description = "Parte do nome", example = "pizzaria")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Categoria do restaurante", example = "PIZZARIA")
            @RequestParam(required = false) String categoria,
            @Parameter(description = "Status do restaurante", example = "ABERTO")
            @RequestParam(required = false) String status,
            @Parameter(description = "true: só os abertos agora (status ABERTO e dentro do horário); false: só os fechados agora")
            @RequestParam(required = false) Boolean abertoAgora,
            @Parameter(description = "Página, começando em 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a " + BuscaService.TAMANHO_MAXIMO + ")", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "campo[,asc|desc]; campos: nome, categoria, taxaEntrega, tempoEstimado",
                    example = "taxaEntrega,asc")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(buscaService.buscarRestaurantes(
                new FiltroRestaurantes(nome, categoria, status, abertoAgora), page, size, sort));
    }

    @GetMapping("/itens")
    @Operation(summary = "Buscar itens (\"Buscar Prato\")",
            description = "Filtros combináveis; textos ignoram maiúsculas e acentos e preços são em centavos. "
                    + "Cada item vem com o restaurante, o preço e a disponibilidade. Se Restaurantes estiver fora "
                    + "do ar, os itens voltam sem os dados do restaurante. Resultado paginado e ordenado "
                    + "(padrão: nome crescente).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de itens encontrados"),
            @ApiResponse(responseCode = "400", description = "Faixa de preço, paginação ou ordenação inválida"),
            @ApiResponse(responseCode = "503", description = "Serviço de Cardápio indisponível")
    })
    public ResponseEntity<PaginaDTO<ItemBuscaDTO>> buscarItens(
            @Parameter(description = "Parte do nome do prato", example = "pizza")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Categoria do produto", example = "PIZZA")
            @RequestParam(required = false) String categoria,
            @Parameter(description = "Preço mínimo em centavos", example = "2000")
            @RequestParam(required = false) Long precoMin,
            @Parameter(description = "Preço máximo em centavos", example = "3000")
            @RequestParam(required = false) Long precoMax,
            @Parameter(description = "Disponibilidade do item", example = "true")
            @RequestParam(required = false) Boolean disponivel,
            @Parameter(description = "Só itens deste restaurante", example = "1")
            @RequestParam(required = false) Long restauranteId,
            @Parameter(description = "Página, começando em 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a " + BuscaService.TAMANHO_MAXIMO + ")", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "campo[,asc|desc]; campos: nome, preco, categoria", example = "preco,desc")
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(buscaService.buscarItens(
                new FiltroItens(nome, categoria, precoMin, precoMax, disponivel, restauranteId), page, size, sort));
    }
}
