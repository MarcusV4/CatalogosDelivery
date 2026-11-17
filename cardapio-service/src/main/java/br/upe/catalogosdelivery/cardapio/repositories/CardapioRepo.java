package br.upe.catalogosdelivery.cardapio.repositories;

import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardapioRepo extends JpaRepository<Cardapio, Long> {
    Optional<Cardapio> findByRestauranteId(Long restauranteId);
}
