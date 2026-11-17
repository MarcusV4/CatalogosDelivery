package br.upe.catalogosdelivery.restaurantes.services;

import br.upe.catalogosdelivery.common.excecoes.RecursoNaoEncontradoException;
import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import br.upe.catalogosdelivery.restaurantes.clients.CardapioClient;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.repositories.RestauranteRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestauranteServiceTest {

    @Mock
    RestauranteRepo restauranteRepo;

    @Mock
    CardapioClient cardapioClient;

    @InjectMocks
    RestauranteService restauranteService;

    @Test
    void cadastroSemOwnerUserIdRetorna400SemSalvar() {
        RestauranteDTO semDono = new RestauranteDTO("Pizzaria", null, null, null, null, null, "Recife", "PE",
                null, null, 5f, null, null, null, null, StatusRestaurante.ABERTO, CategoriaRestaurante.PIZZARIA);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> restauranteService.cadastrarRestaurante(semDono));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(restauranteRepo, never()).save(any());
    }

    @Test
    void excluiOCardapioAntesDoRestaurante() {
        when(restauranteRepo.existsById(1L)).thenReturn(true);

        restauranteService.deletarRestaurante(1L);

        InOrder ordem = inOrder(cardapioClient, restauranteRepo);
        ordem.verify(cardapioClient).excluirCardapioDoRestaurante(1L);
        ordem.verify(restauranteRepo).deleteById(1L);
    }

    @Test
    void naoExcluiORestauranteSeOCardapioEstiverForaDoAr() {
        when(restauranteRepo.existsById(1L)).thenReturn(true);
        doThrow(new ServicoIndisponivelException("Cardápio fora do ar", null))
                .when(cardapioClient).excluirCardapioDoRestaurante(1L);

        assertThrows(ServicoIndisponivelException.class, () -> restauranteService.deletarRestaurante(1L));

        verify(restauranteRepo, never()).deleteById(any());
    }

    @Test
    void restauranteInexistenteRetorna404SemChamarOCardapio() {
        when(restauranteRepo.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> restauranteService.deletarRestaurante(99L));

        verifyNoInteractions(cardapioClient);
    }
}
