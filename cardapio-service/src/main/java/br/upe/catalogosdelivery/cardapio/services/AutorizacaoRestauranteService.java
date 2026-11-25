package br.upe.catalogosdelivery.cardapio.services;

import org.springframework.stereotype.Service;

@Service
public class AutorizacaoRestauranteService {

    /**
     * PENDENTE (#31): verificar se o usuário autenticado é dono do restaurante.
     * Hoje não há autenticação; qualquer chamador pode escrever.
     */
    public void validarPermissao(Long restauranteId) {
        // A integração com Identidade será implementada na #31.
    }
}
