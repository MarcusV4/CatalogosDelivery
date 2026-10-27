package br.upe.catalogosdelivery.mappers;

import br.upe.catalogosdelivery.entities.DTOs.DonoResponseDTO;
import br.upe.catalogosdelivery.entities.Dono;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DonoMapper {
    public DonoResponseDTO toResponseDTO(Dono dono) {
        List<DonoResponseDTO.RestauranteResumoDTO> restaurantes =
                dono.getRestaurante() == null
                        ? List.of()
                        : dono.getRestaurante().stream()
                        .map(this::toRestauranteResumoDTO)
                        .toList();

        return new DonoResponseDTO(
                dono.getId(),
                dono.getNomeDono(),
                dono.getEmailDono(),
                dono.getTelefoneDono(),
                dono.getCpfDono(),
                restaurantes
        );
    }

    private DonoResponseDTO.RestauranteResumoDTO toRestauranteResumoDTO(
            Restaurante restaurante) {

        return new DonoResponseDTO.RestauranteResumoDTO(
                restaurante.getRestauranteId(),
                restaurante.getRestauranteNome(),
                restaurante.getCidade(),
                restaurante.getEstado()
        );
    }
}
