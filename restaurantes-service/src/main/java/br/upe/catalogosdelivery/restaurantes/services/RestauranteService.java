package br.upe.catalogosdelivery.restaurantes.services;

import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.clients.CardapioClient;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.repositories.RestauranteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RestauranteService {


    @Autowired
    RestauranteRepo restauranteRepo;

    @Autowired
    CardapioClient cardapioClient;

    private final RestauranteMapper restauranteMapper = new RestauranteMapper();


    public Restaurante cadastrarRestaurante(RestauranteDTO dto){
        // ownerUserId é obrigatório no banco; sem esta checagem o erro viraria 500.
        // Quando a #31 integrar o JWT, ele passará a vir do `sub` do token.
        if (dto.ownerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ownerUserId (identificador do dono do restaurante).");
        }

        Restaurante restaurante = restauranteMapper.toEntity(dto);

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
