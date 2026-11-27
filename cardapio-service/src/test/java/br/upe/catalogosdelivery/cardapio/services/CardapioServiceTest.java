package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.entities.Cardapio;
import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardapioServiceTest {

    @Mock
    CardapioRepo cardapioRepo;

    @Mock
    AutorizacaoRestauranteService autorizacaoRestauranteService;

    @InjectMocks
    CardapioService cardapioService;

    @Test
    void quemNaoEhDonoNaoExcluiOCardapio() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não é proprietário do restaurante."))
                .when(autorizacaoRestauranteService).validarPermissao(1L);

        assertThrows(ResponseStatusException.class, () -> cardapioService.excluirCardapioDoRestaurante(1L));
        verifyNoInteractions(cardapioRepo);
    }

    @Test
    void marcaOCardapioComoExcluido() {
        Cardapio cardapio = new Cardapio();
        when(cardapioRepo.findByRestauranteId(1L)).thenReturn(Optional.of(cardapio));

        cardapioService.excluirCardapioDoRestaurante(1L);

        assertNotNull(cardapio.getExcluidoEm());
        verify(cardapioRepo).save(cardapio);
    }

    @Test
    void restauranteSemCardapioNaoFazNada() {
        when(cardapioRepo.findByRestauranteId(1L)).thenReturn(Optional.empty());

        cardapioService.excluirCardapioDoRestaurante(1L);

        verify(cardapioRepo, never()).save(any());
    }

    @Test
    void excluirDuasVezesMantemADataOriginal() {
        Instant original = Instant.parse("2026-11-16T12:00:00Z");
        Cardapio cardapio = new Cardapio();
        cardapio.setExcluidoEm(original);
        when(cardapioRepo.findByRestauranteId(1L)).thenReturn(Optional.of(cardapio));

        cardapioService.excluirCardapioDoRestaurante(1L);

        assertEquals(original, cardapio.getExcluidoEm());
        verify(cardapioRepo, never()).save(any());
    }
}
