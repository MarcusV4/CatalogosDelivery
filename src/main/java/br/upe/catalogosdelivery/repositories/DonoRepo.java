package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.Dono;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonoRepo extends JpaRepository<Dono, Long> {
}
