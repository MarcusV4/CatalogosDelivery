package br.upe.catalogosdelivery.restaurantes.services;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.HorarioFuncionamentoDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.SubstituirHorariosDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.CategoriaRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.HorarioFuncionamento;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.exceptions.RestauranteNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.repositories.HorarioFuncRepo;
import br.upe.catalogosdelivery.restaurantes.repositories.RestauranteRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestauranteServiceTest {

    @Mock RestauranteRepo restauranteRepo;
    @Mock HorarioFuncRepo horarioFuncRepo;
    @Spy RestauranteMapper restauranteMapper = new RestauranteMapper();
    @Mock Clock clock;
    @InjectMocks RestauranteService restauranteService;

    @Test
    void cadastroSemOwnerUserIdRetorna400SemSalvar() {
        RestauranteDTO dto = new RestauranteDTO("Pizzaria", null, null, null, null, null, "Recife", "PE",
                null, null, 500L, null, null, null, null, CategoriaRestaurante.PIZZARIA);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> restauranteService.cadastrarRestaurante(dto));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verify(restauranteRepo, never()).save(any());
    }

    @Test
    void atualizarRestauranteRejeitaOutroProprietario() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        AtualizarRestauranteDTO dto = atualizar(12L);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> restauranteService.atualizarRestaurante(1L, dto));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        verify(restauranteRepo, never()).save(any());
    }

    @Test
    void atualizarRestauranteSalvaDadosEPreservaProprietarioCadastrado() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        when(restauranteRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(1L)).thenReturn(List.of());
        when(clock.withZone(any())).thenReturn(Clock.systemUTC());
        restauranteService.atualizarRestaurante(1L, atualizar(10L));
        assertEquals("Nome", restaurante.getRestauranteNome());
        assertEquals(10L, restaurante.getOwnerUserId());
        verify(restauranteRepo).save(restaurante);
    }

    @Test
    void atualizarStatusAlteraEGravaStatus() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        when(restauranteRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(1L)).thenReturn(List.of());
        when(clock.withZone(any())).thenReturn(Clock.systemUTC());
        restauranteService.atualizarStatus(1L, new AtualizarStatusRestauranteDTO(10L, StatusRestaurante.FECHADO));
        assertEquals(StatusRestaurante.FECHADO, restaurante.getStatusRestaurante());
    }

    @Test
    void substituirHorariosRejeitaDiaDuplicadoSemApagarHorariosAtuais() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        HorarioFuncionamentoDTO horario = new HorarioFuncionamentoDTO(DiaSemana.SEGUNDA,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(17, 0));
        SubstituirHorariosDTO dto = new SubstituirHorariosDTO(10L, List.of(horario, horario));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> restauranteService.substituirHorarios(1L, dto)).getStatusCode());
        verify(horarioFuncRepo, never()).deleteAllByIdRestauranteRestauranteId(any());
    }

    @Test
    void substituirHorariosApagaESalvaSemanaInformada() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        HorarioFuncionamentoDTO segunda = new HorarioFuncionamentoDTO(DiaSemana.SEGUNDA,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(17, 0));
        when(horarioFuncRepo.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        List<HorarioFuncionamentoDTO> resultado = restauranteService.substituirHorarios(1L,
                new SubstituirHorariosDTO(10L, List.of(segunda)));
        verify(horarioFuncRepo).deleteAllByIdRestauranteRestauranteId(1L);
        assertEquals(List.of(segunda), resultado);
    }

    @Test
    void fechadoNaoEhAbertoAgoraMesmoDentroDoHorario() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.FECHADO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        Clock fixed = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneOffset.UTC);
        when(clock.withZone(any())).thenReturn(fixed);
        when(horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(1L)).thenReturn(List.of(
                horario(DiaSemana.SEGUNDA, "09:00", "18:00", restaurante)));
        assertFalse(restauranteService.buscarRestaurante(1L).abertoAgora());
    }

    @Test
    void abertoAgoraEhTrueDentroDoHorarioLocal() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        Clock fixed = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneOffset.UTC);
        when(clock.withZone(any())).thenReturn(fixed);
        when(horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(1L)).thenReturn(List.of(
                horario(DiaSemana.SEGUNDA, "09:00", "18:00", restaurante)));
        assertTrue(restauranteService.buscarRestaurante(1L).abertoAgora());
    }

    @Test
    void abertoAgoraConsideraFusoRecifeEIntervaloFechadoNoHorarioDeFechamento() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        Clock fixed = Clock.fixed(Instant.parse("2026-11-16T21:00:00Z"), ZoneOffset.UTC);
        when(clock.withZone(any())).thenReturn(fixed);
        when(horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(1L)).thenReturn(List.of(
                horario(DiaSemana.SEGUNDA, "16:00", "18:00", restaurante)));
        assertFalse(restauranteService.buscarRestaurante(1L).abertoAgora());
    }

    @Test
    void deletarFazExclusaoLogicaERejeitaUsuarioDiferente() {
        Restaurante restaurante = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> restauranteService.deletarRestaurante(1L, 11L)).getStatusCode());
        verify(restauranteRepo, never()).save(any());
        restauranteService.deletarRestaurante(1L, 10L);
        assertEquals(StatusRestaurante.INDISPONIVEL, restaurante.getStatusRestaurante());
        verify(restauranteRepo).save(restaurante);
    }

    @Test
    void cadastroSemNomeRetorna400SemSalvar() {
        for (String nome : new String[]{null, "  "}) {
            RestauranteDTO dto = new RestauranteDTO(nome, null, null, null, null, null, "Recife", "PE",
                    null, null, 500L, null, null, null, 10L, CategoriaRestaurante.PIZZARIA);
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                    () -> restauranteService.cadastrarRestaurante(dto)).getStatusCode());
        }
        verify(restauranteRepo, never()).save(any());
    }

    @Test
    void cadastroComecaComStatusAbertoENormalizaEstado() {
        RestauranteDTO dto = new RestauranteDTO("Pizzaria", "Desc", "Rua A", "10", null, "Centro", "Recife", "pe",
                "50000-000", "81999990000", 500L, 20, 40, null, 10L, CategoriaRestaurante.PIZZARIA);
        when(restauranteRepo.save(any(Restaurante.class))).thenAnswer(inv -> inv.getArgument(0));

        Restaurante salvo = restauranteService.cadastrarRestaurante(dto);

        assertEquals(StatusRestaurante.ABERTO, salvo.getStatusRestaurante());
        assertEquals("PE", salvo.getEstado());
    }

    @Test
    void atualizarSemNomeRetorna400SemSalvar() {
        when(restauranteRepo.findById(1L)).thenReturn(Optional.of(restaurante(1L, 10L, StatusRestaurante.ABERTO)));
        AtualizarRestauranteDTO dto = new AtualizarRestauranteDTO(" ", null, null, null, null, null, "Recife", "PE",
                null, null, 0L, null, null, null, 10L, CategoriaRestaurante.PIZZARIA);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> restauranteService.atualizarRestaurante(1L, dto)).getStatusCode());
        verify(restauranteRepo, never()).save(any());
    }

    @Test
    void listarIgnoraDesativadosEBuscaHorariosNumaUnicaConsulta() {
        Restaurante aberto = restaurante(1L, 10L, StatusRestaurante.ABERTO);
        Restaurante fechado = restaurante(2L, 10L, StatusRestaurante.FECHADO);
        when(restauranteRepo.findAllByStatusRestauranteIsNullOrStatusRestauranteNot(StatusRestaurante.INDISPONIVEL))
                .thenReturn(List.of(aberto, fechado));
        Clock fixed = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneOffset.UTC);
        when(clock.withZone(any())).thenReturn(fixed);
        when(horarioFuncRepo.findAllByIdRestauranteRestauranteIdIn(List.of(1L, 2L))).thenReturn(List.of(
                horario(DiaSemana.SEGUNDA, "09:00", "18:00", aberto),
                horario(DiaSemana.SEGUNDA, "09:00", "18:00", fechado)));

        List<RestauranteResponseDTO> resultado = restauranteService.listarRestaurante();

        assertEquals(List.of(1L, 2L), resultado.stream().map(RestauranteResponseDTO::restauranteId).toList());
        assertTrue(resultado.get(0).abertoAgora());
        assertFalse(resultado.get(1).abertoAgora());
        verify(restauranteRepo, never()).findAll();
        verify(horarioFuncRepo, never()).findAllByIdRestauranteRestauranteIdOrderByDiaSemana(any());
    }

    @Test
    void listarSemRestaurantesNaoConsultaHorarios() {
        when(restauranteRepo.findAllByStatusRestauranteIsNullOrStatusRestauranteNot(StatusRestaurante.INDISPONIVEL))
                .thenReturn(List.of());
        assertEquals(List.of(), restauranteService.listarRestaurante());
        verifyNoInteractions(horarioFuncRepo);
    }

    @Test
    void restauranteInexistenteRetorna404() {
        when(restauranteRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RestauranteNaoEncontradoException.class, () -> restauranteService.buscarRestaurante(99L));
    }

    private Restaurante restaurante(Long id, Long owner, StatusRestaurante status) {
        Restaurante restaurante = new Restaurante();
        restaurante.setRestauranteId(id);
        restaurante.setOwnerUserId(owner);
        restaurante.setStatusRestaurante(status);
        restaurante.setTaxaEntrega(0L);
        return restaurante;
    }

    private AtualizarRestauranteDTO atualizar(Long ownerId) {
        return new AtualizarRestauranteDTO("Nome", null, null, null, null, null, "Recife", "PE", null, null,
                0L, null, null, null, ownerId, CategoriaRestaurante.PIZZARIA);
    }

    private HorarioFuncionamento horario(DiaSemana dia, String inicio, String fim, Restaurante restaurante) {
        HorarioFuncionamento horario = new HorarioFuncionamento();
        horario.setDiaSemana(dia);
        horario.setHoraAbertura(java.time.LocalTime.parse(inicio));
        horario.setHoraFechamento(java.time.LocalTime.parse(fim));
        horario.setIdRestaurante(restaurante);
        return horario;
    }
}
