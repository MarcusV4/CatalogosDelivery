package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.Produto;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepo extends JpaRepository<Produto, Long> {
}
