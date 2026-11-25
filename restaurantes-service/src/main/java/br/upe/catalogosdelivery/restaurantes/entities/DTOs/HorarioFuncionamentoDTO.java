package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record HorarioFuncionamentoDTO(

        @NotNull(message = "O dia da semana deve ser informado.")
        DiaSemana diaSemana,

        @NotNull(message = "A hora de abertura deve ser informada.")
        LocalTime horaAbertura,

        @NotNull(message = "A hora de fechamento deve ser informada.")
        LocalTime horaFechamento
) {}
