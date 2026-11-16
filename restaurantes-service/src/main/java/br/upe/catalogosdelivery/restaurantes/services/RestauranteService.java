package br.upe.catalogosdelivery.restaurantes.services;

import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.clients.CardapioClient;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Dono;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.repositories.DonoRepo;
import br.upe.catalogosdelivery.restaurantes.repositories.RestauranteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestauranteService {


    @Autowired
    RestauranteRepo restauranteRepo;

    @Autowired
    DonoRepo donoRepo;

    @Autowired
    CardapioClient cardapioClient;

    private final RestauranteMapper restauranteMapper = new RestauranteMapper();


    public Restaurante cadastrarRestaurante(RestauranteDTO dto){
        Dono dono = donoRepo.findById(dto.idDono())
                .orElseThrow(() ->
                        new RuntimeException("Dono não encontrado")
                );

        Restaurante restaurante = restauranteMapper.toEntity(dto);

        restaurante.setRestauranteDono(dono);

        // O cardápio agora pertence ao serviço de Cardápio, que o cria no primeiro
        // produto cadastrado depois de validar o restaurante aqui via HTTP.
        return restauranteRepo.save(restaurante);
    }


    public List<RestauranteResponseDTO> listarRestaurante(){
        return restauranteRepo.findAll().stream().map(restauranteMapper::toResponseDTO).toList();
    }

    public RestauranteResponseDTO buscarRestaurante(Long idRestaurante){
        Restaurante restaurante = restauranteRepo.findById(idRestaurante).orElseThrow(()-> new RecursoNaoEncontradoException("Restaurante inexistente!") );
        RestauranteResponseDTO restauranteDTO = restauranteMapper.toResponseDTO(restaurante);
        return restauranteDTO;
    }

    public void deletarRestaurante(Long idRestaurante){
        boolean restauranteExiste = restauranteRepo.existsById(idRestaurante);

        if(!restauranteExiste) throw new RecursoNaoEncontradoException("Restaurante não existe");

        // Primeiro o Cardápio exclui o cardápio e os produtos do restaurante. Se ele estiver
        // fora do ar, a chamada lança 503 e o restaurante NÃO é excluído: os dois serviços
        // continuam consistentes e a exclusão pode ser repetida depois.
        cardapioClient.excluirCardapioDoRestaurante(idRestaurante);

        restauranteRepo.deleteById(idRestaurante);
    }
}
