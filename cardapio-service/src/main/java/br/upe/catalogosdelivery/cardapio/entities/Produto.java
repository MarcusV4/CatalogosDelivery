package br.upe.catalogosdelivery.cardapio.entities;

import br.upe.catalogosdelivery.cardapio.entities.Enums.CategoriaProduto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "Preço em centavos (R$ 25,90 = 2590)", example = "2590", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long preco;

    private boolean disponivel;

    private String urlImagem;

    @Enumerated(EnumType.STRING)
    private CategoriaProduto categoria;

    @ManyToOne
    @JoinColumn(name = "cardapio_id")
    private Cardapio cardapio;


}
