package br.upe.catalogosdelivery.entities;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Dono {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String nomeDono;

    private String emailDono;

    private String telefoneDono;

    private String cpfDono;

    @OneToMany(mappedBy = "restauranteDono",cascade = CascadeType.ALL)
    private List<Restaurante> restaurante;
}
