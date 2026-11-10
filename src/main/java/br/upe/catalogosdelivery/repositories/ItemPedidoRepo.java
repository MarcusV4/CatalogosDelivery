package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPedidoRepo extends JpaRepository<ItemPedido, Long> {
}
