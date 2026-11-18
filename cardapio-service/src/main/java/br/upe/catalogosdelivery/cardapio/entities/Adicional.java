package br.upe.catalogosdelivery.cardapio.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Adicional {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "adicional_seq")
    @SequenceGenerator(name = "adicional_seq", sequenceName = "adicional_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Long preco;

    @Column(nullable = false)
    private boolean disponivel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;
}
