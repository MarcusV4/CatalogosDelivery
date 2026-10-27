package br.upe.catalogosdelivery.mappers;

import br.upe.catalogosdelivery.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.entities.Restaurante;
import org.springframework.stereotype.Component;

@Component
public class RestauranteMapper {

    public Restaurante toEntity(RestauranteDTO dto) {
        Restaurante restaurante = new Restaurante();

        restaurante.setRestauranteNome(dto.restauranteNome());
        restaurante.setRestauranteDesc(dto.restauranteDesc());
        restaurante.setLogradouro(dto.logradouro());
        restaurante.setNumero(dto.numero());
        restaurante.setComplemento(dto.complemento());
        restaurante.setBairro(dto.bairro());
        restaurante.setCidade(dto.cidade());
        restaurante.setEstado(dto.estado());
        restaurante.setCep(dto.cep());
        restaurante.setRestauranteTelefone(dto.restauranteTelefone());
        restaurante.setTaxaEntrega(dto.taxaEntrega());
        restaurante.setTempoEstimadoMin(dto.tempoEstimadoMin());
        restaurante.setTempoEstimadoMax(dto.tempoEstimadoMax());
        restaurante.setUrlImagem(dto.urlImagem());
        restaurante.setStatusRestaurante(dto.statusRestaurante());
        restaurante.setCategoriaRestaurante(dto.categoriaRestaurante());

        return restaurante;
    }


    public RestauranteResponseDTO toResponseDTO(Restaurante restaurante) {
        return new RestauranteResponseDTO(
                restaurante.getRestauranteId(),
                restaurante.getRestauranteNome(),
                restaurante.getRestauranteDesc(),
                restaurante.getCidade(),
                restaurante.getEstado(),
                restaurante.getUrlImagem(),
                restaurante.getRestauranteDono().getId(),
                restaurante.getRestauranteDono().getNomeDono(),
                restaurante.getStatusRestaurante(),
                restaurante.getCategoriaRestaurante()
        );
    }

}
