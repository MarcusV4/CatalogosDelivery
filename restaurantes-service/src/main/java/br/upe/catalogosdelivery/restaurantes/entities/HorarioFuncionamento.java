package br.upe.catalogosdelivery.restaurantes.entities;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private LocalTime horaAbertura;

    private LocalTime horaFechamento;

    @Enumerated(EnumType.STRING)
    private DiaSemana diaSemana;

    @ManyToOne
    private Restaurante idRestaurante;
}
