package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepo extends JpaRepository<Restaurante, Long> {
}
