package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.EnderecoDono;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoDonoRepo extends JpaRepository<EnderecoDono, Long> {
}
