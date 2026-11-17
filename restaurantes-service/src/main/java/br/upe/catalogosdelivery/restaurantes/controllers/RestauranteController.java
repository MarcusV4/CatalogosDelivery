package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.HorarioFuncionamentoDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.SubstituirHorariosDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurantes")
@Tag(name = "Restaurantes", description = "Cadastro e consulta de restaurantes")
public class RestauranteController {

    private final RestauranteService restauranteService;
    private final RestauranteMapper restauranteMapper;

    public RestauranteController(RestauranteService restauranteService, RestauranteMapper restauranteMapper) {
        this.restauranteService = restauranteService;
        this.restauranteMapper = restauranteMapper;
    }

    @PostMapping
    @Operation(summary = "Cadastrar restaurante")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Restaurante cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")})
    public ResponseEntity<RestauranteResponseDTO> cadastrarRestaurante(@RequestBody RestauranteDTO dto) {
        Restaurante restaurante = restauranteService.cadastrarRestaurante(dto);
        return ResponseEntity.status(201).body(restauranteMapper.toResponseDTO(restaurante));
    }

    @GetMapping
    @Operation(summary = "Listar restaurantes")
    @ApiResponse(responseCode = "200", description = "Restaurantes encontrados")
    public ResponseEntity<List<RestauranteResponseDTO>> listarRestaurante() {
        return ResponseEntity.ok(restauranteService.listarRestaurante());
    }

    @GetMapping("/{idRestaurante}")
    @Operation(summary = "Consultar restaurante por ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Restaurante encontrado"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<RestauranteResponseDTO> buscarRestaurante(@PathVariable Long idRestaurante) {
        return ResponseEntity.ok(restauranteService.buscarRestaurante(idRestaurante));
    }

    @PutMapping("/{idRestaurante}")
    @Operation(summary = "Atualizar dados do restaurante", description = "Substitui os dados cadastrais. ownerUserId identifica o proprietário nesta etapa de integração.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Restaurante atualizado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<RestauranteResponseDTO> atualizarRestaurante(@PathVariable Long idRestaurante,
                                                                        @RequestBody AtualizarRestauranteDTO dto) {
        return ResponseEntity.ok(restauranteService.atualizarRestaurante(idRestaurante, dto));
    }

    @PatchMapping("/{idRestaurante}/status")
    @Operation(summary = "Alterar status do restaurante")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<RestauranteResponseDTO> atualizarStatus(@PathVariable Long idRestaurante,
                                                                   @RequestBody AtualizarStatusRestauranteDTO dto) {
        return ResponseEntity.ok(restauranteService.atualizarStatus(idRestaurante, dto));
    }

    @GetMapping("/{idRestaurante}/horarios")
    @Operation(summary = "Consultar horários de funcionamento")
    public ResponseEntity<List<HorarioFuncionamentoDTO>> listarHorarios(@PathVariable Long idRestaurante) {
        return ResponseEntity.ok(restauranteService.listarHorarios(idRestaurante));
    }

    @PutMapping("/{idRestaurante}/horarios")
    @Operation(summary = "Substituir horários de funcionamento", description = "Substitui a semana completa. Dias omitidos ficam fechados; não são aceitos intervalos que cruzam a meia-noite.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Horários atualizados"),
            @ApiResponse(responseCode = "400", description = "Horários inválidos"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário")})
    public ResponseEntity<List<HorarioFuncionamentoDTO>> substituirHorarios(@PathVariable Long idRestaurante,
                                                                            @RequestBody SubstituirHorariosDTO dto) {
        return ResponseEntity.ok(restauranteService.substituirHorarios(idRestaurante, dto));
    }

    @DeleteMapping("/{idRestaurante}")
    @Operation(summary = "Desativar restaurante", description = "Exclusão lógica: altera o status para INDISPONIVEL.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Restaurante desativado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<Void> deletarRestaurante(@PathVariable Long idRestaurante,
                                                    @RequestParam Long ownerUserId) {
        restauranteService.deletarRestaurante(idRestaurante, ownerUserId);
        return ResponseEntity.noContent().build();
    }
}
