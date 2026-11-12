package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.AvaliacaoRestaurante;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRestauranteRepo extends JpaRepository<AvaliacaoRestaurante, Long> {
}
