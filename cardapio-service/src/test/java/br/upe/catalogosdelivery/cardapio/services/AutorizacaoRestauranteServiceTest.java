package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.clients.RestauranteClient;
import br.upe.catalogosdelivery.cardapio.clients.RestauranteResumoDTO;
import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAtual;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutorizacaoRestauranteServiceTest {

    private static final String DONO = "6f1c2a9e-1b2d-4c3e-8f4a-5b6c7d8e9f01";
    private static final String OUTRA_CONTA = "0a9b8c7d-6e5f-4a3b-9c2d-1e0f9a8b7c6d";

    private final RestauranteClient restauranteClient = mock(RestauranteClient.class);
    private final UsuarioAtual usuarioAtual = mock(UsuarioAtual.class);
    private final AutorizacaoRestauranteService service =
            new AutorizacaoRestauranteService(restauranteClient, usuarioAtual);

    @Test
    void donoDoRestauranteTemPermissao() {
        logadoComo(DONO.toUpperCase());
        when(restauranteClient.buscarRestaurante(10L)).thenReturn(restaurante(DONO));

        assertDoesNotThrow(() -> service.validarPermissao(10L));
    }

    @Test
    void outraContaDeLojaRecebe403() {
        logadoComo(OUTRA_CONTA);
        when(restauranteClient.buscarRestaurante(10L)).thenReturn(restaurante(DONO));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.validarPermissao(10L));
        assertEquals(HttpStatus.FORBIDDEN, erro.getStatusCode());
        assertEquals("Usuário não é proprietário do restaurante.", erro.getReason());
    }

    @Test
    void restauranteSemDonoNaoPodeSerAlterado() {
        logadoComo(DONO);

        assertThrows(ResponseStatusException.class, () -> service.validarDono(restaurante(null)));
        assertThrows(ResponseStatusException.class, () -> service.validarDono(null));
    }

    @Test
    void contaQueNaoEhDeLojaRecebe403DoUsuarioAtual() {
        when(usuarioAtual.obterLoja()).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente lojas."));

        assertThrows(ResponseStatusException.class, () -> service.validarDono(restaurante(DONO)));
    }

    @Test
    void restauranteInexistenteResponde404() {
        when(restauranteClient.buscarRestaurante(99L))
                .thenThrow(new RecursoNaoEncontradoException("Restaurante 99 não encontrado."));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.validarPermissao(99L));
    }

    private void logadoComo(String conta) {
        when(usuarioAtual.obterLoja()).thenReturn(new UsuarioAutenticado(conta, "LOJA"));
    }

    private static RestauranteResumoDTO restaurante(String dono) {
        return new RestauranteResumoDTO(10L, "Pizzaria", "ABERTO", 0L, dono);
    }
}
