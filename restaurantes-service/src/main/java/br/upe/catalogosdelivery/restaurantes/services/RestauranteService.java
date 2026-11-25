package br.upe.catalogosdelivery.restaurantes.services;

import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.HorarioFuncionamentoDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.RestauranteResponseDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.SubstituirHorariosDTO;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import br.upe.catalogosdelivery.restaurantes.entities.Enums.StatusRestaurante;
import br.upe.catalogosdelivery.restaurantes.entities.HorarioFuncionamento;
import br.upe.catalogosdelivery.restaurantes.entities.Restaurante;
import br.upe.catalogosdelivery.restaurantes.exceptions.RestauranteNaoEncontradoException;
import br.upe.catalogosdelivery.restaurantes.mappers.RestauranteMapper;
import br.upe.catalogosdelivery.restaurantes.repositories.HorarioFuncRepo;
import br.upe.catalogosdelivery.restaurantes.repositories.RestauranteRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RestauranteService {

    private final RestauranteRepo restauranteRepo;
    private final HorarioFuncRepo horarioFuncRepo;
    private final RestauranteMapper restauranteMapper;
    private final Clock clock;

    @Value("${catalogo.horarios.zone-id:America/Recife}")
    private String zoneId;

    public RestauranteService(RestauranteRepo restauranteRepo, HorarioFuncRepo horarioFuncRepo,
                              RestauranteMapper restauranteMapper, Clock clock) {
        this.restauranteRepo = restauranteRepo;
        this.horarioFuncRepo = horarioFuncRepo;
        this.restauranteMapper = restauranteMapper;
        this.clock = clock;
    }

    public Restaurante cadastrarRestaurante(RestauranteDTO dto) {
        if (dto.ownerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ownerUserId.");
        }
        validarDados(dto.restauranteNome(), dto.taxaEntrega(), dto.tempoEstimadoMin(), dto.tempoEstimadoMax());
        return restauranteRepo.save(restauranteMapper.toEntity(dto));
    }

    /** Lista os restaurantes ativos; os desativados (INDISPONIVEL) seguem acessíveis só por ID. */
    public List<RestauranteResponseDTO> listarRestaurante() {
        List<Restaurante> restaurantes = restauranteRepo
                .findAllByStatusRestauranteIsNullOrStatusRestauranteNot(StatusRestaurante.INDISPONIVEL);
        if (restaurantes.isEmpty()) {
            return List.of();
        }
        // Uma única consulta de horários para a lista inteira, em vez de uma por restaurante.
        Map<Long, List<HorarioFuncionamento>> horariosPorRestaurante = horarioFuncRepo
                .findAllByIdRestauranteRestauranteIdIn(restaurantes.stream().map(Restaurante::getRestauranteId).toList())
                .stream()
                .collect(Collectors.groupingBy(h -> h.getIdRestaurante().getRestauranteId()));
        return restaurantes.stream()
                .map(r -> toResponse(r, horariosPorRestaurante.getOrDefault(r.getRestauranteId(), List.of())))
                .toList();
    }

    /** Lança 404 se o restaurante não existir. Usado antes de ler e validar o corpo da requisição. */
    public void verificarExistencia(Long id) {
        if (!restauranteRepo.existsById(id)) {
            throw new RestauranteNaoEncontradoException(id);
        }
    }

    public RestauranteResponseDTO buscarRestaurante(Long idRestaurante) {
        return toResponse(buscarEntidade(idRestaurante));
    }

    @Transactional
    public RestauranteResponseDTO atualizarRestaurante(Long id, AtualizarRestauranteDTO dto) {
        Restaurante restaurante = buscarEntidade(id);
        validarProprietario(restaurante, dto.ownerUserId());
        validarDados(dto.restauranteNome(), dto.taxaEntrega(), dto.tempoEstimadoMin(), dto.tempoEstimadoMax());
        restaurante.setRestauranteNome(dto.restauranteNome());
        restaurante.setRestauranteDesc(dto.restauranteDesc());
        restaurante.setLogradouro(dto.logradouro());
        restaurante.setNumero(dto.numero());
        restaurante.setComplemento(dto.complemento());
        restaurante.setBairro(dto.bairro());
        restaurante.setCidade(dto.cidade());
        restaurante.setEstado(dto.estado() == null ? null : dto.estado().toUpperCase(Locale.ROOT));
        restaurante.setCep(dto.cep());
        restaurante.setRestauranteTelefone(dto.restauranteTelefone());
        restaurante.setTaxaEntrega(dto.taxaEntrega());
        restaurante.setTempoEstimadoMin(dto.tempoEstimadoMin());
        restaurante.setTempoEstimadoMax(dto.tempoEstimadoMax());
        restaurante.setUrlImagem(dto.urlImagem());
        restaurante.setCategoriaRestaurante(dto.categoriaRestaurante());
        return toResponse(restauranteRepo.save(restaurante));
    }

    @Transactional
    public RestauranteResponseDTO atualizarStatus(Long id, AtualizarStatusRestauranteDTO dto) {
        Restaurante restaurante = buscarEntidade(id);
        validarProprietario(restaurante, dto.ownerUserId());
        if (dto.statusRestaurante() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe statusRestaurante.");
        }
        restaurante.setStatusRestaurante(dto.statusRestaurante());
        return toResponse(restauranteRepo.save(restaurante));
    }

    public List<HorarioFuncionamentoDTO> listarHorarios(Long id) {
        buscarEntidade(id);
        return horarioFuncRepo.findAllByIdRestauranteRestauranteIdOrderByDiaSemana(id).stream()
                .map(h -> new HorarioFuncionamentoDTO(h.getDiaSemana(), h.getHoraAbertura(), h.getHoraFechamento()))
                .toList();
    }

    @Transactional
    public List<HorarioFuncionamentoDTO> substituirHorarios(Long id, SubstituirHorariosDTO dto) {
        Restaurante restaurante = buscarEntidade(id);
        validarProprietario(restaurante, dto.ownerUserId());
        if (dto.horarios() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a lista de horarios.");
        }
        Set<DiaSemana> dias = EnumSet.noneOf(DiaSemana.class);
        for (HorarioFuncionamentoDTO horario : dto.horarios()) {
            if (horario == null || horario.diaSemana() == null || horario.horaAbertura() == null
                    || horario.horaFechamento() == null || !horario.horaAbertura().isBefore(horario.horaFechamento())
                    || !dias.add(horario.diaSemana())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada dia deve ser único e ter horários válidos, com abertura anterior ao fechamento.");
            }
        }
        horarioFuncRepo.deleteAllByIdRestauranteRestauranteId(id);
        List<HorarioFuncionamento> entidades = dto.horarios().stream().map(h -> {
            HorarioFuncionamento horario = new HorarioFuncionamento();
            horario.setDiaSemana(h.diaSemana());
            horario.setHoraAbertura(h.horaAbertura());
            horario.setHoraFechamento(h.horaFechamento());
            horario.setIdRestaurante(restaurante);
            return horario;
        }).toList();
        return horarioFuncRepo.saveAll(entidades).stream()
                .map(h -> new HorarioFuncionamentoDTO(h.getDiaSemana(), h.getHoraAbertura(), h.getHoraFechamento()))
                .toList();
    }

    @Transactional
    public void deletarRestaurante(Long id, Long ownerUserId) {
        Restaurante restaurante = buscarEntidade(id);
        validarProprietario(restaurante, ownerUserId);
        restaurante.setStatusRestaurante(StatusRestaurante.INDISPONIVEL);
        restauranteRepo.save(restaurante);
    }

    private Restaurante buscarEntidade(Long id) {
        return restauranteRepo.findById(id).orElseThrow(() -> new RestauranteNaoEncontradoException(id));
    }

    private void validarProprietario(Restaurante restaurante, Long ownerUserId) {
        if (ownerUserId == null || !ownerUserId.equals(restaurante.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não é proprietário do restaurante.");
        }
    }

    private void validarDados(String nome, Long taxa, Integer tempoMin, Integer tempoMax) {
        if (nome == null || nome.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe restauranteNome.");
        }
        if (taxa == null || taxa < 0 || tempoMin != null && tempoMin < 0 || tempoMax != null && tempoMax < 0
                || tempoMin != null && tempoMax != null && tempoMin > tempoMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taxa ou tempo estimado inválido.");
        }
    }

    private RestauranteResponseDTO toResponse(Restaurante restaurante) {
        return toResponse(restaurante, horarioFuncRepo
                .findAllByIdRestauranteRestauranteIdOrderByDiaSemana(restaurante.getRestauranteId()));
    }

    private RestauranteResponseDTO toResponse(Restaurante restaurante, List<HorarioFuncionamento> horarios) {
        RestauranteResponseDTO dto = restauranteMapper.toResponseDTO(restaurante);
        ZoneId zone = ZoneId.of(zoneId == null ? "America/Recife" : zoneId);
        LocalDate hoje = LocalDate.now(clock.withZone(zone));
        LocalTime agora = LocalTime.now(clock.withZone(zone));
        DiaSemana diaHoje = switch (hoje.getDayOfWeek()) {
            case SUNDAY -> DiaSemana.DOMINGO;
            case MONDAY -> DiaSemana.SEGUNDA;
            case TUESDAY -> DiaSemana.TERCA;
            case WEDNESDAY -> DiaSemana.QUARTA;
            case THURSDAY -> DiaSemana.QUINTA;
            case FRIDAY -> DiaSemana.SEXTA;
            case SATURDAY -> DiaSemana.SABADO;
        };
        boolean abertoAgora = restaurante.getStatusRestaurante() == StatusRestaurante.ABERTO
                && horarios.stream().anyMatch(h -> h.getDiaSemana() == diaHoje
                && !agora.isBefore(h.getHoraAbertura()) && agora.isBefore(h.getHoraFechamento()));
        return new RestauranteResponseDTO(dto.restauranteId(), dto.restauranteNome(), dto.restauranteDesc(),
                dto.logradouro(), dto.numero(), dto.complemento(), dto.bairro(), dto.cidade(), dto.estado(), dto.cep(),
                dto.restauranteTelefone(), dto.urlImagem(), dto.ownerUserId(), dto.statusRestaurante(),
                dto.categoriaRestaurante(), dto.taxaEntrega(), dto.tempoEstimadoMin(), dto.tempoEstimadoMax(), abertoAgora);
    }
}
