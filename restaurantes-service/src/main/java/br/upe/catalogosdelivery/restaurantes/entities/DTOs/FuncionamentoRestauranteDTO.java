package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Funcionamento atual do restaurante, consultado pelo Checkout antes de fechar o pedido")
public record FuncionamentoRestauranteDTO(
        @Schema(description = "Status cadastrado pelo dono", example = "ABERTO") StatusRestaurante status,
        @Schema(description = "Status ABERTO e o horário atual dentro de um dos horários de hoje", example = "true")
        boolean abertoAgora,
        @Schema(description = "Dia de hoje no fuso dos horários (America/Recife)", example = "SEGUNDA")
        DiaSemana diaSemana,
        @Schema(description = "Horários de hoje; vazio quando o restaurante não abre neste dia")
        List<HorarioFuncionamentoDTO> horarios
) {
}
