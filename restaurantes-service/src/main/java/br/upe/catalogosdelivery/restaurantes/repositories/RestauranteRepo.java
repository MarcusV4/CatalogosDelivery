package br.upe.catalogosdelivery.restaurantes.repositories;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestauranteRepo extends JpaRepository<Restaurante, Long> {
    /** Restaurantes visíveis na listagem: exclui os desativados (exclusão lógica). */
    List<Restaurante> findAllByStatusRestauranteIsNullOrStatusRestauranteNot(StatusRestaurante status);
}
