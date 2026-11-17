package br.upe.catalogosdelivery.restaurantes.repositories;

import br.upe.catalogosdelivery.restaurantes.entities.HorarioFuncionamento;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioFuncRepo extends JpaRepository<HorarioFuncionamento, Long> {
    java.util.List<HorarioFuncionamento> findAllByIdRestauranteRestauranteIdOrderByDiaSemana(Long restauranteId);
    void deleteAllByIdRestauranteRestauranteId(Long restauranteId);
}
