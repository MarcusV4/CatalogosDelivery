package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.clients.RestauranteResumoDTO;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAtual;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verificação de propriedade das escritas do cardápio (RF16): só o dono do restaurante
 * (JWT.sub == ownerUserId, lido do serviço de Restaurantes) altera produtos e adicionais.
 * O token já foi validado na Identidade pelo AutenticacaoInterceptor.
 */
@Service
@RequiredArgsConstructor
public class AutorizacaoRestauranteService {

    private final RestauranteClient restauranteClient;
    private final UsuarioAtual usuarioAtual;

    /** Consulta o restaurante no serviço de Restaurantes (404/503 se não existir ou estiver fora do ar). */
    public void validarPermissao(Long restauranteId) {
        validarDono(restauranteClient.buscarRestaurante(restauranteId));
    }

    /** Para quando o restaurante já foi consultado, evitando uma segunda chamada HTTP. */
    public void validarDono(RestauranteResumoDTO restaurante) {
        UsuarioAutenticado usuario = usuarioAtual.obter();
        if (restaurante == null || restaurante.ownerUserId() == null
                || !restaurante.ownerUserId().equals(usuario.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não é proprietário do restaurante.");
        }
    }
}
