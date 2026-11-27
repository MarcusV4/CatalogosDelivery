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
 * Propriedade nas escritas do cardápio: só a conta LOJA dona do restaurante altera seus
 * produtos e adicionais. O dono é lido do serviço de Restaurantes (o Cardápio não guarda
 * essa informação) e comparado com o sub do token, já validado pelo AutenticacaoInterceptor.
 */
@Service
@RequiredArgsConstructor
public class AutorizacaoRestauranteService {

    private final RestauranteClient restauranteClient;
    private final UsuarioAtual usuarioAtual;

    /** Consulta o restaurante no serviço de Restaurantes: 404 se não existir, 503 se estiver fora do ar. */
    public void validarPermissao(Long restauranteId) {
        validarDono(restauranteClient.buscarRestaurante(restauranteId));
    }

    /** Para quando o restaurante já foi consultado, evitando uma segunda chamada HTTP. */
    public void validarDono(RestauranteResumoDTO restaurante) {
        UsuarioAutenticado usuario = usuarioAtual.obterLoja();
        if (restaurante == null || restaurante.ownerUserId() == null
                || !restaurante.ownerUserId().equalsIgnoreCase(usuario.contaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não é proprietário do restaurante.");
        }
    }
}
