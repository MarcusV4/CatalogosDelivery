package br.upe.catalogosdelivery.cardapio.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cardapio {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long idCardapio;

    // Referência ao restaurante do serviço de Restaurantes (outro banco: sem FK, só o id).
    @Column(name = "restaurante_id", unique = true, nullable = false)
    private Long restauranteId;

    // Preenchido quando o restaurante é excluído no serviço de Restaurantes (exclusão lógica):
    // os produtos deixam de aparecer nas listagens, na busca e na validação para o Checkout.
    private Instant excluidoEm;

    @OneToMany(mappedBy = "cardapio")
    private List<Produto> produtos = new ArrayList<>();
}
