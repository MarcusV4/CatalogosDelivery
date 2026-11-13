package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.Cardapio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardapioRepo extends JpaRepository<Cardapio, Long> {
    Optional<Cardapio> findByRestaurante_restauranteId(Long restauranteId);
}
