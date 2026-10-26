package br.upe.catalogosdelivery.entities;


import br.upe.catalogosdelivery.entities.Enums.StatusRestaurante;
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
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long restauranteId;

    private String restauranteNome;

    private String restauranteDesc;

    private String restauranteEndereco;

    private String restauranteTelefone;

    @ManyToOne(cascade = CascadeType.ALL)
    private Dono restauranteDono;

    @Enumerated(EnumType.STRING)
    private StatusRestaurante statusRestaurante;
}
