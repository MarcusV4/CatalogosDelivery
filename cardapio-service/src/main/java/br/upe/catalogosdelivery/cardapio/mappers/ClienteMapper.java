package br.upe.catalogosdelivery.cardapio.mappers;

import br.upe.catalogosdelivery.cardapio.entities.Cliente;
import br.upe.catalogosdelivery.cardapio.entities.DTOs.ClienteDTO;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public Cliente toEntity(ClienteDTO dto) {

        Cliente cliente = new Cliente();

        cliente.setNome(dto.nome());
        cliente.setEmail(dto.email());
        cliente.setTelefone(dto.telefone());

        return cliente;
    }
}
