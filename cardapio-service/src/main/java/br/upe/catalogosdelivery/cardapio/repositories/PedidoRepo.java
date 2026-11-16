package br.upe.catalogosdelivery.cardapio.repositories;

import br.upe.catalogosdelivery.cardapio.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepo extends JpaRepository<Pedido, Long> {
}
