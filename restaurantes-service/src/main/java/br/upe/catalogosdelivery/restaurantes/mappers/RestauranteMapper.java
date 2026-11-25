package br.upe.catalogosdelivery.restaurantes.mappers;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RestauranteMapper {

    /** @param ownerUserId o {@code sub} do token de quem está cadastrando */
    public Restaurante toEntity(RestauranteDTO dto, Long ownerUserId) {
        Restaurante restaurante = new Restaurante();

        restaurante.setRestauranteNome(dto.restauranteNome());
        restaurante.setRestauranteDesc(dto.restauranteDesc());
        restaurante.setLogradouro(dto.logradouro());
        restaurante.setNumero(dto.numero());
        restaurante.setComplemento(dto.complemento());
        restaurante.setBairro(dto.bairro());
        restaurante.setCidade(dto.cidade());
        restaurante.setEstado(dto.estado() == null ? null : dto.estado().toUpperCase(Locale.ROOT));
        restaurante.setCep(dto.cep());
        restaurante.setRestauranteTelefone(dto.restauranteTelefone());
        restaurante.setTaxaEntrega(dto.taxaEntrega());
        restaurante.setTempoEstimadoMin(dto.tempoEstimadoMin());
        restaurante.setTempoEstimadoMax(dto.tempoEstimadoMax());
        restaurante.setUrlImagem(dto.urlImagem());
        restaurante.setOwnerUserId(ownerUserId);
        // O status não vem no cadastro: todo restaurante novo começa ABERTO.
        // "abertoAgora" só fica true depois que houver horário cadastrado para o dia.
        restaurante.setStatusRestaurante(StatusRestaurante.ABERTO);
        restaurante.setCategoriaRestaurante(dto.categoriaRestaurante());

        return restaurante;
    }


    public RestauranteResponseDTO toResponseDTO(Restaurante restaurante) {
        return new RestauranteResponseDTO(
                restaurante.getRestauranteId(),
                restaurante.getRestauranteNome(),
                restaurante.getRestauranteDesc(),
                restaurante.getLogradouro(),
                restaurante.getNumero(),
                restaurante.getComplemento(),
                restaurante.getBairro(),
                restaurante.getCidade(),
                restaurante.getEstado(),
                restaurante.getCep(),
                restaurante.getRestauranteTelefone(),
                restaurante.getUrlImagem(),
                restaurante.getOwnerUserId(),
                restaurante.getStatusRestaurante(),
                restaurante.getCategoriaRestaurante(),
                restaurante.getTaxaEntrega(),
                restaurante.getTempoEstimadoMin(),
                restaurante.getTempoEstimadoMax(),
                false
        );
    }

}
