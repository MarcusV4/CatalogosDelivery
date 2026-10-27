package br.upe.catalogosdelivery.controllers;

import br.upe.catalogosdelivery.entities.DTOs.DonoResponseDTO;
import br.upe.catalogosdelivery.entities.Dono;
import br.upe.catalogosdelivery.entities.Restaurante;
import br.upe.catalogosdelivery.mappers.DonoMapper;
import br.upe.catalogosdelivery.services.DonoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donos")
public class DonoController {

    @Autowired
    DonoService donosService;
    @Autowired
    private DonoMapper donoMapper;

    @PostMapping
    public ResponseEntity<DonoResponseDTO> cadastrarDonos(@RequestBody Dono dono) {
        Dono dono1 = donosService.cadastrarDono(dono);

        DonoResponseDTO resposta = donoMapper.toResponseDTO(dono1);

        return ResponseEntity.status(200).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<DonoResponseDTO>> listarDonos() {
        List<DonoResponseDTO> donos = donosService.listarDonos();
        return ResponseEntity.status(200).body(donos);
    }

    @GetMapping("/{idDono}")
    public ResponseEntity<DonoResponseDTO> buscarDonos(@PathVariable Long idDono) {
        return ResponseEntity.ok(donosService.buscarDono(idDono));
    }

    @DeleteMapping("/{idDono}")
    public ResponseEntity<Map<String, String>> deletarDono(@PathVariable Long idDono) {
        donosService.deletarDono(idDono);

        Map<String, String> resposta = new HashMap<>();
        resposta.put("mensagem", "Dono deletado com sucesso!");

        return ResponseEntity.ok(resposta);

    }
}
