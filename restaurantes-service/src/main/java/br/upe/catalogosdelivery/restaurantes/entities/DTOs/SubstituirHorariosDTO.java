package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubstituirHorariosDTO(

        @NotNull(message = "O ID do proprietário deve ser informado.")
        Long ownerUserId,

        @NotEmpty(message = "Informe pelo menos um horário de funcionamento.")
        List<@NotNull @Valid HorarioFuncionamentoDTO> horarios
) {}
