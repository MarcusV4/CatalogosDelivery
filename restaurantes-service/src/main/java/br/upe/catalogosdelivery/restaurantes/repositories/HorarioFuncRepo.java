package br.upe.catalogosdelivery.restaurantes.repositories;

import br.upe.catalogosdelivery.restaurantes.entities.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HorarioFuncRepo extends JpaRepository<HorarioFuncionamento, Long> {
    List<HorarioFuncionamento> findAllByIdRestauranteRestauranteIdOrderByDiaSemana(Long restauranteId);
    List<HorarioFuncionamento> findAllByIdRestauranteRestauranteIdIn(Collection<Long> restauranteIds);
    void deleteAllByIdRestauranteRestauranteId(Long restauranteId);
}
