package br.upe.catalogosdelivery.restaurantes.services;

import br.upe.catalogosdelivery.common.excecoes.ServicoIndisponivelException;
import br.upe.catalogosdelivery.restaurantes.clients.CardapioClient;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.exceptions.RestauranteNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.repositories.RestauranteRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

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
    void cadastraRestauranteValidoComOsDadosDoDono() {
        RestauranteDTO dto = restauranteValido();
        when(restauranteRepo.save(any(Restaurante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Restaurante resultado = restauranteService.cadastrarRestaurante(dto);

        assertEquals("Pizzaria", resultado.getRestauranteNome());
        assertEquals(7L, resultado.getOwnerUserId());
        assertEquals(550L, resultado.getTaxaEntrega());
        verify(restauranteRepo).save(any(Restaurante.class));
    }

    @Test
    void listaRestaurantesConvertidosParaResponseDTO() {
        Restaurante restaurante = restaurantePersistido();
        when(restauranteRepo.findAll()).thenReturn(List.of(restaurante));

        List<RestauranteResponseDTO> resultado = restauranteService.listarRestaurante();

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).restauranteId());
        assertEquals("Pizzaria", resultado.get(0).restauranteNome());
        assertEquals(7L, resultado.get(0).ownerUserId());
    }

    @Test
    void buscaRestauranteExistenteConvertidoParaResponseDTO() {
        when(restauranteRepo.findById(10L)).thenReturn(Optional.of(restaurantePersistido()));

        RestauranteResponseDTO resultado = restauranteService.buscarRestaurante(10L);

        assertEquals(10L, resultado.restauranteId());
        assertEquals("Pizzaria", resultado.restauranteNome());
        assertEquals(550L, resultado.taxaEntrega());
    }

    @Test
    void cadastroSemOwnerUserIdRetorna400SemSalvar() {
        RestauranteDTO semDono = new RestauranteDTO("Pizzaria", null, null, null, null, null, "Recife", "PE",
                null, null, 500L, null, null, null, null, StatusRestaurante.ABERTO, CategoriaRestaurante.PIZZARIA);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> restauranteService.cadastrarRestaurante(semDono));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(restauranteRepo, never()).save(any());
    }

    @Test
    void cadastroComTaxaNegativaRetorna400SemSalvar() {
        RestauranteDTO taxaNegativa = new RestauranteDTO("Pizzaria", null, null, null, null, null, "Recife", "PE",
                null, null, -1L, null, null, null, 1L, StatusRestaurante.ABERTO, CategoriaRestaurante.PIZZARIA);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> restauranteService.cadastrarRestaurante(taxaNegativa));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(restauranteRepo, never()).save(any());
    }

    @Test
    void buscarRestauranteInexistenteLancaRestauranteNaoEncontrado() {
        when(restauranteRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestauranteNaoEncontradoException.class, () -> restauranteService.buscarRestaurante(99L));
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

        assertThrows(RestauranteNaoEncontradoException.class, () -> restauranteService.deletarRestaurante(99L));

        verifyNoInteractions(cardapioClient);
    }

    private static RestauranteDTO restauranteValido() {
        return new RestauranteDTO("Pizzaria", "Pizza artesanal", "Rua A", "10", null, "Centro",
                "Recife", "PE", "50000-000", "81999999999", 550L, 30, 45, null, 7L,
                StatusRestaurante.ABERTO, CategoriaRestaurante.PIZZARIA);
    }

    private static Restaurante restaurantePersistido() {
        Restaurante restaurante = new Restaurante();
        restaurante.setRestauranteId(10L);
        restaurante.setRestauranteNome("Pizzaria");
        restaurante.setRestauranteDesc("Pizza artesanal");
        restaurante.setCidade("Recife");
        restaurante.setEstado("PE");
        restaurante.setOwnerUserId(7L);
        restaurante.setStatusRestaurante(StatusRestaurante.ABERTO);
        restaurante.setCategoriaRestaurante(CategoriaRestaurante.PIZZARIA);
        restaurante.setTaxaEntrega(550L);
        return restaurante;
    }
}
