package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.HorarioFuncionamento;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioFuncRepo extends JpaRepository<HorarioFuncionamento, Long> {
}
