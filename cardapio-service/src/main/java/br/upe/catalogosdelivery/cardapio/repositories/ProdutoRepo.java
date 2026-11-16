package br.upe.catalogosdelivery.cardapio.repositories;

import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepo extends JpaRepository<Produto, Long> {
    List<Produto> findByCardapio_RestauranteId(Long restauranteId);
}
