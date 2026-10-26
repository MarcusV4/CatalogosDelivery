package br.upe.catalogosdelivery.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemCardapio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemNome;

    private String itemDesc;

    private Integer itemPreco;

    private boolean itemDisponivel;

    @ManyToOne
    private Restaurante restaurante;

}
