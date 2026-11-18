package br.upe.catalogosdelivery.cardapio.repositories;

import br.upe.catalogosdelivery.cardapio.entities.Adicional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdicionalRepo extends JpaRepository<Adicional, Long> {
    List<Adicional> findByProduto_IdOrderById(Long produtoId);
    Optional<Adicional> findByIdAndProduto_Id(Long id, Long produtoId);
}
