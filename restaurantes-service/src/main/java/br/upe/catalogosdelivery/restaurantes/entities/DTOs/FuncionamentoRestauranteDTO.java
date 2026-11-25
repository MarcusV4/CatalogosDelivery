package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;

import java.util.List;

public record FuncionamentoRestauranteDTO(
        StatusRestaurante status,
        boolean abertoAgora,
        DiaSemana diaSemana,
        List<HorarioFuncionamentoDTO> horarios
) {
}