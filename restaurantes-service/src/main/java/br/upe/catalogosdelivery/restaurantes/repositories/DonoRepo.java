package br.upe.catalogosdelivery.restaurantes.repositories;

import br.upe.catalogosdelivery.restaurantes.entities.Dono;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonoRepo extends JpaRepository<Dono, Long> {
}
