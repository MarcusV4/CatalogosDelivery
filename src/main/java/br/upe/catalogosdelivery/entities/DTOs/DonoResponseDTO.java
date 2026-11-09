package br.upe.catalogosdelivery.entities.DTOs;

import java.util.List;

public record DonoResponseDTO(
        Long id,
        String nomeDono,
        String emailDono,
        String telefoneDono,
        String cpfDono,
        List<RestauranteResumoDTO> restaurantes
) {
    public record RestauranteResumoDTO(
            Long restauranteId,
            String restauranteNome,
            String cidade,
            String estado
    ) {}
}
