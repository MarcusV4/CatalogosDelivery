package br.upe.catalogosdelivery.controllers;

import br.upe.catalogosdelivery.entities.DTOs.PedidoRequestDTO;
import br.upe.catalogosdelivery.entities.DTOs.PedidoResponseDTO;
import br.upe.catalogosdelivery.entities.Pedido;
import br.upe.catalogosdelivery.services.PedidoService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> realizarPedido(
            @RequestBody PedidoRequestDTO dto) {
        PedidoResponseDTO pedido = pedidoService.realizarPedido(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @GetMapping("/{idPedido}")
    public ResponseEntity<PedidoResponseDTO> buscarPedidoPorId(
            @PathVariable Long idPedido){

        PedidoResponseDTO pedido = pedidoService.buscarPedidoPorId(idPedido);

        return ResponseEntity.ok(pedido);
    }

}
