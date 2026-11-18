package br.upe.catalogosdelivery.restaurantes.entities.DTOs;

import java.util.List;

public record SubstituirHorariosDTO(Long ownerUserId, List<HorarioFuncionamentoDTO> horarios) {}
