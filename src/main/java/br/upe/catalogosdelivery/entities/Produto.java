package br.upe.catalogosdelivery.entities;

import br.upe.catalogosdelivery.entities.Enums.CategoriaProduto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String nome;

    private String descricao;

    private BigDecimal preco;

    private boolean disponivel;

    private String urlImagem;

    private CategoriaProduto categoria;

    @ManyToOne
    @JoinColumn(name = "cardapio_id")
    private Cardapio cardapio;


}
