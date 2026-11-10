package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepo extends JpaRepository<Cliente, Long> {
}
