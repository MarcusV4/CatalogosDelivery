package br.upe.catalogosdelivery.entities;

import br.upe.catalogosdelivery.entities.Enums.NotaAvaliacaoRestaurante;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoRestaurante {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long idAvaliacaoRestaurante;

    private String comentario;

    private Timestamp criadaEm;

    private Timestamp atualizadaEm;

    @ManyToOne
    private Dono idDono;

    @ManyToOne
    private Restaurante idRestaurante;

    @Enumerated(EnumType.STRING)
    private NotaAvaliacaoRestaurante nota;
}
