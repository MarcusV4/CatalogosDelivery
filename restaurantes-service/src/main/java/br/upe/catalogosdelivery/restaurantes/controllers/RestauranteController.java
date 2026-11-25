package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.common.seguranca.UsuarioAtual;
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
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurantes")
@Tag(name = "Restaurantes", description = "Cadastro e consulta de restaurantes")
public class RestauranteController {

    private final RestauranteService restauranteService;
    private final RestauranteMapper restauranteMapper;
    private final UsuarioAtual usuarioAtual;

    public RestauranteController(RestauranteService restauranteService, RestauranteMapper restauranteMapper,
                                 UsuarioAtual usuarioAtual) {
        this.restauranteService = restauranteService;
        this.restauranteMapper = restauranteMapper;
        this.usuarioAtual = usuarioAtual;
    }

    @PostMapping
    @Operation(summary = "Cadastrar restaurante", description = "O proprietário (ownerUserId) é o sub do token; não é aceito no corpo. Exige perfil de loja (role).")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Restaurante cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Perfil do usuário não pode cadastrar restaurantes")})
    public ResponseEntity<RestauranteResponseDTO> cadastrarRestaurante(@Valid @RequestBody RestauranteDTO dto) {
        Restaurante restaurante = restauranteService.cadastrarRestaurante(dto, usuarioAtual.obter());
        return ResponseEntity.status(201).body(restauranteMapper.toResponseDTO(restaurante));
    }

    @GetMapping
    @Operation(summary = "Listar restaurantes", description = "Não inclui restaurantes desativados (INDISPONIVEL), que continuam acessíveis por ID.")
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
    @Operation(summary = "Atualizar dados do restaurante", description = "Substitui os dados cadastrais. Só o proprietário (sub do token igual a ownerUserId) pode alterar.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Restaurante atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<RestauranteResponseDTO> atualizarRestaurante(@PathVariable Long idRestaurante,
                                                                        @Valid @RequestBody AtualizarRestauranteDTO dto) {
        return ResponseEntity.ok(restauranteService.atualizarRestaurante(idRestaurante, dto, usuarioAtual.obter().id()));
    }

    @PatchMapping("/{idRestaurante}/status")
    @Operation(summary = "Alterar status do restaurante")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<RestauranteResponseDTO> atualizarStatus(@PathVariable Long idRestaurante,
                                                                   @Valid @RequestBody AtualizarStatusRestauranteDTO dto) {
        return ResponseEntity.ok(restauranteService.atualizarStatus(idRestaurante, dto, usuarioAtual.obter().id()));
    }

    @GetMapping("/{idRestaurante}/horarios")
    @Operation(summary = "Consultar horários de funcionamento")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Horários da semana; dias ausentes estão fechados"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<List<HorarioFuncionamentoDTO>> listarHorarios(@PathVariable Long idRestaurante) {
        return ResponseEntity.ok(restauranteService.listarHorarios(idRestaurante));
    }

    @PutMapping("/{idRestaurante}/horarios")
    @Operation(summary = "Substituir horários de funcionamento", description = "Substitui a semana completa. Dias omitidos ficam fechados; não são aceitos intervalos que cruzam a meia-noite.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Horários atualizados"),
            @ApiResponse(responseCode = "400", description = "Horários inválidos"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<List<HorarioFuncionamentoDTO>> substituirHorarios(@PathVariable Long idRestaurante,
                                                                            @Valid @RequestBody SubstituirHorariosDTO dto) {
        return ResponseEntity.ok(restauranteService.substituirHorarios(idRestaurante, dto, usuarioAtual.obter().id()));
    }

    @DeleteMapping("/{idRestaurante}")
    @Operation(summary = "Desativar restaurante", description = "Exclusão lógica: altera o status para INDISPONIVEL. O restaurante sai da listagem, segue acessível por ID e pode ser reativado com PATCH /status.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Restaurante desativado"),
            @ApiResponse(responseCode = "403", description = "Usuário não é proprietário"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")})
    public ResponseEntity<Void> deletarRestaurante(@PathVariable Long idRestaurante) {
        restauranteService.deletarRestaurante(idRestaurante, usuarioAtual.obter().id());
        return ResponseEntity.noContent().build();
    }
}
