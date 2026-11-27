package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** O dono não vem no corpo: é a conta do token (Authorization: Bearer). */
public record SubstituirHorariosDTO(

        // Lista vazia é permitida: os dias omitidos ficam fechados.
        @NotNull(message = "Informe a lista de horários.")
        List<@NotNull(message = "O horário não pode ser nulo.") @Valid HorarioFuncionamentoDTO> horarios
) {}
