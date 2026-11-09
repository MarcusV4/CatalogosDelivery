package br.upe.catalogosdelivery.services;

import br.upe.catalogosdelivery.entities.DTOs.DonoResponseDTO;
import br.upe.catalogosdelivery.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.entities.Dono;
import br.upe.catalogosdelivery.entities.Restaurante;
import br.upe.catalogosdelivery.mappers.DonoMapper;
import br.upe.catalogosdelivery.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.repositories.DonoRepo;
import br.upe.catalogosdelivery.repositories.RestauranteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DonoService {

    @Autowired
    DonoRepo donoRepo;

    @Autowired
    RestauranteRepo restauranteRepo;

    private final DonoMapper donoMapper = new DonoMapper();

    public Dono cadastrarDono(Dono dono) {
        return donoRepo.save(dono);
    }

    public List<DonoResponseDTO> listarDonos() {
        return donoRepo.findAll().stream().map(donoMapper::toResponseDTO).toList();
    }

    public DonoResponseDTO buscarDono(Long idDono) {
        Dono dono = donoRepo.findById(idDono).orElseThrow(()-> new RuntimeException("Dono inexistente!") );
        DonoResponseDTO donoResponseDTO = donoMapper.toResponseDTO(dono);
        return donoResponseDTO;
    }

    public void deletarDono(Long idDono){
        boolean donoExiste = donoRepo.existsById(idDono);

        if(!donoExiste) throw new RuntimeException("Dono não existe");


        donoRepo.deleteById(idDono);
    }
}
