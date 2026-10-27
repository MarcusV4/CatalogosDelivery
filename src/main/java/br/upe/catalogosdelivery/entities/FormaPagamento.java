package br.upe.catalogosdelivery.entities;


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
public class FormaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long idFormaPagamento;

    private String apelido;

    private String bandeira;

    private String ultimos4Dig;

    @ManyToOne
    private Dono donoId;

}
