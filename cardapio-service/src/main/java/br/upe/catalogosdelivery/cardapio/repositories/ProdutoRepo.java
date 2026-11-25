package br.upe.catalogosdelivery.cardapio.repositories;

import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.entities.Produto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepo extends JpaRepository<Produto, Long> {
    // Só produtos de cardápios ativos (restaurante não excluído).
    // Os adicionais vêm na mesma consulta, evitando uma consulta extra por produto.
    @EntityGraph(attributePaths = "adicionais")
    List<Produto> findByCardapio_RestauranteIdAndCardapio_ExcluidoEmIsNull(Long restauranteId);

    @EntityGraph(attributePaths = "adicionais")
    List<Produto> findByCardapio_ExcluidoEmIsNull();
}
