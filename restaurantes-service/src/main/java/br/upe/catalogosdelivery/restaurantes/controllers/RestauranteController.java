package br.upe.catalogosdelivery.restaurantes.controllers;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.services.RestauranteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurantes")
@Tag(name = "Restaurantes", description = "Cadastro e consulta de restaurantes")
public class RestauranteController {

    @Autowired
    RestauranteService restauranteService;
    @Autowired
    private RestauranteMapper restauranteMapper;


    @PostMapping
    @Operation(summary = "Cadastrar restaurante")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restaurante cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex.: sem ownerUserId ou JSON malformado)")
    })
    public ResponseEntity<RestauranteResponseDTO> cadastrarRestaurante(@RequestBody RestauranteDTO restauranteDTO) {
        Restaurante restaurante1 = restauranteService.cadastrarRestaurante(restauranteDTO);

        RestauranteResponseDTO restauranteResposta = restauranteMapper.toResponseDTO(restaurante1);

        return ResponseEntity.status(201).body(restauranteResposta);
    }

    @GetMapping
    @Operation(summary = "Listar restaurantes")
    @ApiResponse(responseCode = "200", description = "Restaurantes encontrados")
    public ResponseEntity<List<RestauranteResponseDTO>> listarRestaurante() {
        List<RestauranteResponseDTO> restaurantes = restauranteService.listarRestaurante();
        return ResponseEntity.status(200).body(restaurantes);
    }

    @GetMapping("/{idRestaurante}")
    @Operation(summary = "Consultar restaurante por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurante encontrado"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente")
    })
    public ResponseEntity<RestauranteResponseDTO> buscarRestaurante(@PathVariable Long idRestaurante) {
        return ResponseEntity.ok(restauranteService.buscarRestaurante(idRestaurante));
    }

    @DeleteMapping("/{idRestaurante}")
    @Operation(summary = "Excluir restaurante", description = "Solicita a exclusão lógica do cardápio antes de remover o restaurante.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurante excluído"),
            @ApiResponse(responseCode = "404", description = "Restaurante inexistente"),
            @ApiResponse(responseCode = "503", description = "Serviço de Cardápio indisponível")
    })
    public ResponseEntity<Map<String, String>> deletarRestaurante(@PathVariable Long idRestaurante) {
        restauranteService.deletarRestaurante(idRestaurante);

        Map<String, String> resposta = new HashMap<>();
        resposta.put("mensagem", "Restaurante deletado com sucesso!");

        return ResponseEntity.ok(resposta);

    }
}
