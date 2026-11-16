package br.upe.catalogosdelivery.cardapio.repositories;

import br.upe.catalogosdelivery.cardapio.entities.Adicional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdicionalRepo extends JpaRepository<Adicional, Long> {
}
