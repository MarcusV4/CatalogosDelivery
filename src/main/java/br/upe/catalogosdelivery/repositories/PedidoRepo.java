package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepo extends JpaRepository<Pedido, Long> {
}
