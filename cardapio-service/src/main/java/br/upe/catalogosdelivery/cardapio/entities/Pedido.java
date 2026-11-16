package br.upe.catalogosdelivery.cardapio.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private LocalDateTime dataPedido;

    private BigDecimal valorTotal;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "forma_pagamento_id")
    private FormaPagamento formaPagamento;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<ItemPedido> itens = new ArrayList<>();

    // Referência ao restaurante do serviço de Restaurantes (outro banco: sem FK, só o id).
    @Column(name = "restaurante_id")
    private Long restauranteId;

    // Nome do restaurante no momento do pedido, obtido via HTTP do serviço de Restaurantes.
    private String nomeRestaurante;

    @ManyToOne
    @JoinColumn(name = "cardapio_id")
    private Cardapio cardapio;

    private BigDecimal taxaEntrega;
}