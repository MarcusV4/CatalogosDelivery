package br.upe.catalogosdelivery.services;

import br.upe.catalogosdelivery.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.entities.Dono;
import br.upe.catalogosdelivery.entities.Restaurante;
import br.upe.catalogosdelivery.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.repositories.DonoRepo;
import br.upe.catalogosdelivery.repositories.RestauranteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestauranteService {


    @Autowired
    RestauranteRepo restauranteRepo;

    @Autowired
    DonoRepo donoRepo;

    private final RestauranteMapper restauranteMapper = new RestauranteMapper();


    public Restaurante cadastrarRestaurante(RestauranteDTO dto){
        Dono dono = donoRepo.findById(dto.idDono())
                .orElseThrow(() ->
                        new RuntimeException("Dono não encontrado")
                );

        Restaurante restaurante = restauranteMapper.toEntity(dto);

        restaurante.setRestauranteDono(dono);

        return restauranteRepo.save(restaurante);
    }


    public List<RestauranteResponseDTO> listarRestaurante(){
        return restauranteRepo.findAll().stream().map(restauranteMapper::toResponseDTO).toList();
    }

    public RestauranteResponseDTO buscarRestaurante(Long idRestaurante){
        Restaurante restaurante = restauranteRepo.findById(idRestaurante).orElseThrow(()-> new RuntimeException("Restaurante inexistente!") );
        RestauranteResponseDTO restauranteDTO = restauranteMapper.toResponseDTO(restaurante);
        return restauranteDTO;
    }

    public void deletarRestaurante(Long idRestaurante){
        boolean restauranteExiste = restauranteRepo.existsById(idRestaurante);

        if(!restauranteExiste) throw new RuntimeException("Restaurante não existe");


        restauranteRepo.deleteById(idRestaurante);
    }
}
