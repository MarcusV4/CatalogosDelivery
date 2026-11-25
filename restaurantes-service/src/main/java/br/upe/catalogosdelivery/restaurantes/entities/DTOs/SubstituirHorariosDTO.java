package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubstituirHorariosDTO(

        // Lista vazia é permitida: os dias omitidos ficam fechados.
        @NotNull(message = "Informe a lista de horários.")
        List<@NotNull(message = "O horário não pode ser nulo.") @Valid HorarioFuncionamentoDTO> horarios
) {}
