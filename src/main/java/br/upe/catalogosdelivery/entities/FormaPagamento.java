package br.upe.catalogosdelivery.entities;


import br.upe.catalogosdelivery.entities.Enums.BandeiraCartao;
import br.upe.catalogosdelivery.entities.Enums.TipoFormaPagamento;
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

    @Enumerated(EnumType.STRING)
    private TipoFormaPagamento tipoFormaPagamento;

    @Enumerated(EnumType.STRING)
    private BandeiraCartao bandeira;

    private String ultimos4Dig;

}
