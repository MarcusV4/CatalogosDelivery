package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;

import java.time.LocalTime;

public record HorarioFuncionamentoDTO(
        DiaSemana diaSemana,
        LocalTime horaAbertura,
        LocalTime horaFechamento
) {}
