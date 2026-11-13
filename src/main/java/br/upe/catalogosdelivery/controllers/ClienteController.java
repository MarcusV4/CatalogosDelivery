package br.upe.catalogosdelivery.controllers;

import br.upe.catalogosdelivery.entities.Cliente;
import br.upe.catalogosdelivery.entities.DTOs.ClienteDTO;
import br.upe.catalogosdelivery.services.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    ClienteService clienteService;

    @PostMapping
    public ResponseEntity<Cliente> cadastrarCliente(
            @RequestBody ClienteDTO dto) {

        Cliente cliente = clienteService.cadastrarCliente(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(cliente);
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listarClientes(){
        return ResponseEntity.ok(clienteService.listarClientes());
    }
}
