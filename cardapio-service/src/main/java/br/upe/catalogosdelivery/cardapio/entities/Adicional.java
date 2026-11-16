package br.upe.catalogosdelivery.cardapio.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Adicional {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    private String nome;

    private float preco;

    private boolean disponivel;

    // Referência ao restaurante do serviço de Restaurantes (outro banco: sem FK, só o id).
    @Column(name = "restaurante_id")
    private Long restauranteId;
}
