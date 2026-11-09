package br.upe.catalogosdelivery.controllers;

import br.upe.catalogosdelivery.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.entities.Restaurante;
import br.upe.catalogosdelivery.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.services.RestauranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurantes")
public class RestauranteController {

    @Autowired
    RestauranteService restauranteService;
    @Autowired
    private RestauranteMapper restauranteMapper;


    @PostMapping
    public ResponseEntity<RestauranteResponseDTO> cadastrarRestaurante(@RequestBody RestauranteDTO restauranteDTO) {
        Restaurante restaurante1 = restauranteService.cadastrarRestaurante(restauranteDTO);

        RestauranteResponseDTO restauranteResposta = restauranteMapper.toResponseDTO(restaurante1);

        return ResponseEntity.status(201).body(restauranteResposta);
    }

    @GetMapping
    public ResponseEntity<List<RestauranteResponseDTO>> listarRestaurante() {
        List<RestauranteResponseDTO> restaurantes = restauranteService.listarRestaurante();
        return ResponseEntity.status(200).body(restaurantes);
    }

    @GetMapping("/{idRestaurante}")
    public ResponseEntity<RestauranteResponseDTO> buscarRestaurante(@PathVariable Long idRestaurante) {
        return ResponseEntity.ok(restauranteService.buscarRestaurante(idRestaurante));
    }

    @DeleteMapping("/{idRestaurante}")
    public ResponseEntity<Map<String, String>> deletarRestaurante(@PathVariable Long idRestaurante) {
        restauranteService.deletarRestaurante(idRestaurante);

        Map<String, String> resposta = new HashMap<>();
        resposta.put("mensagem", "Restaurante deletado com sucesso!");

        return ResponseEntity.ok(resposta);

    }
}
