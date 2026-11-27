package br.upe.catalogosdelivery.restaurantes.entities;


import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Taxa fixa informada pelo restaurante, em centavos; não é cálculo de frete", example = "550", minimum = "0")
    private Long taxaEntrega;

    private Integer tempoEstimadoMin;

    private Integer tempoEstimadoMax;

    private String urlImagem;

    /** UUID da conta dona (sub do token da Identidade). Ver V2__owner_user_id_uuid.sql. */
    @Column(nullable = false, length = 36)
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    private StatusRestaurante statusRestaurante;

    @Enumerated(EnumType.STRING)
    private CategoriaRestaurante categoriaRestaurante;
}
