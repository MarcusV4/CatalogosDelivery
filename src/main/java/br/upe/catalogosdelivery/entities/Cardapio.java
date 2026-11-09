package br.upe.catalogosdelivery.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @OneToOne
    @JoinColumn(name = "restaurante_id", unique = true)
    private Restaurante restaurante;

    @OneToMany(mappedBy = "cardapio")
    private List<Produto> produtos = new ArrayList<>();
}
