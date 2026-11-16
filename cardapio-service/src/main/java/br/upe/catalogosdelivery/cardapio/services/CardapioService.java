package br.upe.catalogosdelivery.cardapio.services;

import br.upe.catalogosdelivery.cardapio.repositories.CardapioRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CardapioService {

    private static final Logger log = LoggerFactory.getLogger(CardapioService.class);

    private final CardapioRepo cardapioRepo;

    /**
     * Chamado pelo serviço de Restaurantes antes de excluir um restaurante.
     * Idempotente: se o restaurante não tem cardápio, ou ele já foi excluído, não faz nada.
     */
    public void excluirCardapioDoRestaurante(Long restauranteId) {
        cardapioRepo.findByRestauranteId(restauranteId)
                .filter(cardapio -> cardapio.getExcluidoEm() == null)
                .ifPresent(cardapio -> {
                    cardapio.setExcluidoEm(Instant.now());
                    cardapioRepo.save(cardapio);
                    log.info("[Cardápio] cardápio do restaurante {} excluído a pedido do serviço de Restaurantes", restauranteId);
                });
    }
}
