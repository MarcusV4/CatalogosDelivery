package br.upe.catalogosdelivery.restaurantes.repositories;

import br.upe.catalogosdelivery.restaurantes.entities.AvaliacaoRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRestauranteRepo extends JpaRepository<AvaliacaoRestaurante, Long> {
}
