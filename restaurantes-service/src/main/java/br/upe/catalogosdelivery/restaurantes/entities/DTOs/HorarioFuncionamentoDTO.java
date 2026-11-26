package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record HorarioFuncionamentoDTO(

        @Schema(example = "SEGUNDA")
        @NotNull(message = "O dia da semana deve ser informado.")
        DiaSemana diaSemana,

        @Schema(type = "string", example = "11:00:00")
        @NotNull(message = "A hora de abertura deve ser informada.")
        LocalTime horaAbertura,

        @Schema(type = "string", example = "23:00:00")
        @NotNull(message = "A hora de fechamento deve ser informada.")
        LocalTime horaFechamento
) {}
