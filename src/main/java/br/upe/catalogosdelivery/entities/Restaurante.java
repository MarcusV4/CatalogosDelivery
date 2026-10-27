package br.upe.catalogosdelivery.entities;


import br.upe.catalogosdelivery.entities.Enums.CategoriaRestaurante;
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

    private String logradouro;

    private String numero;

    private String complemento;

    private String bairro;

    private String cidade;

    private String estado;

    private String cep;

    private String restauranteTelefone;

    private float taxaEntrega;

    private Integer tempoEstimadoMin;

    private Integer tempoEstimadoMax;

    private String urlImagem;

    @ManyToOne
    @JoinColumn(name = "idDono", nullable = false)
    private Dono restauranteDono;

    @Enumerated(EnumType.STRING)
    private StatusRestaurante statusRestaurante;

    @Enumerated(EnumType.STRING)
    private CategoriaRestaurante categoriaRestaurante;
}
