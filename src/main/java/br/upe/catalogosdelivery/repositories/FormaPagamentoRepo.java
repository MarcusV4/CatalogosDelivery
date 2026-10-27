package br.upe.catalogosdelivery.repositories;

import br.upe.catalogosdelivery.entities.FormaPagamento;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormaPagamentoRepo extends JpaRepository<FormaPagamento, Long> {
}
