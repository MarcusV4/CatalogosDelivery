package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.Cardapio;
import br.upe.catalogosdelivery.entities.Produto;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepo extends JpaRepository<Produto, Long> {
    List<Produto> findByCardapio_Restaurante_restauranteId(Long restauranteId);
}
