package br.upe.catalogosdelivery.services;

import br.upe.catalogosdelivery.entities.Cliente;
import br.upe.catalogosdelivery.entities.DTOs.ClienteDTO;
import br.upe.catalogosdelivery.mappers.ClienteMapper;
import br.upe.catalogosdelivery.repositories.ClienteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    @Autowired
    ClienteRepo clienteRepo;

    private final ClienteMapper clienteMapper =  new ClienteMapper();

    public Cliente cadastrarCliente(ClienteDTO dto) {

        Cliente cliente = clienteMapper.toEntity(dto);

        return clienteRepo.save(cliente);
    }

    public List<Cliente> listarClientes(){
        return clienteRepo.findAll();
    }
}
