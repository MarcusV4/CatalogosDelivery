package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.clients.RestauranteResumoDTO;
import br.upe.catalogosdelivery.common.excecoes.NaoAutenticadoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAtual;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** Regra de propriedade do cardápio: JWT.sub == ownerUserId do restaurante. */
@ExtendWith(MockitoExtension.class)
class AutorizacaoRestauranteServiceTest {

    @Mock RestauranteClient restauranteClient;
    @Mock UsuarioAtual usuarioAtual;
    @InjectMocks AutorizacaoRestauranteService autorizacao;

    @Test
    void donoDoRestaurantePodeEscrever() {
        when(usuarioAtual.obter()).thenReturn(new UsuarioAutenticado(42L, "LOJISTA"));
        when(restauranteClient.buscarRestaurante(10L)).thenReturn(restaurante(42L));

        assertDoesNotThrow(() -> autorizacao.validarPermissao(10L));
    }

    @Test
    void outroUsuarioRecebe403() {
        when(usuarioAtual.obter()).thenReturn(new UsuarioAutenticado(99L, "LOJISTA"));
        when(restauranteClient.buscarRestaurante(10L)).thenReturn(restaurante(42L));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> autorizacao.validarPermissao(10L));

        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        assertEquals("Usuário não é proprietário do restaurante.", erro.getReason());
    }

    @Test
    void restauranteSemDonoInformadoRecebe403() {
        when(usuarioAtual.obter()).thenReturn(new UsuarioAutenticado(42L, "LOJISTA"));

        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> autorizacao.validarDono(restaurante(null))).getStatusCode());
    }

    @Test
    void semUsuarioAutenticadoRecebe401() {
        when(usuarioAtual.obter()).thenThrow(new NaoAutenticadoException("Token de autenticação ausente."));

        assertThrows(NaoAutenticadoException.class, () -> autorizacao.validarDono(restaurante(42L)));
    }

    @Test
    void restaurantesForaDoArPropaga503() {
        when(restauranteClient.buscarRestaurante(10L))
                .thenThrow(new ServicoIndisponivelException("O serviço de Restaurantes está indisponível.", null));

        assertThrows(ServicoIndisponivelException.class, () -> autorizacao.validarPermissao(10L));
    }

    private static RestauranteResumoDTO restaurante(Long dono) {
        return new RestauranteResumoDTO(10L, "Pizzaria", "ABERTO", 0L, dono);
    }
}
