package br.upe.catalogosdelivery.restaurantes.services;

import br.upe.catalogosdelivery.common.excecoes.AcessoNegadoException;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAtual;
import br.upe.catalogosdelivery.common.seguranca.UsuarioAutenticado;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.AtualizarStatusRestauranteDTO;
import br.upe.catalogosdelivery.restaurantes.entities.DTOs.FuncionamentoRestauranteDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RestauranteService {

    private static final Logger log = LoggerFactory.getLogger(RestauranteService.class);

    /**
     * Ordem de declaração de {@link DiaSemana} (DOMINGO a SABADO): enums se comparam pelo ordinal, não pelo
     * texto gravado no banco. Um dia nulo (dado legado) vai para o fim em vez de gerar erro.
     */
    private static final Comparator<HorarioFuncionamento> ORDEM_DA_SEMANA = Comparator.comparing(
            HorarioFuncionamento::getDiaSemana, Comparator.nullsLast(Comparator.naturalOrder()));

    private final RestauranteRepo restauranteRepo;
    private final HorarioFuncRepo horarioFuncRepo;
    private final RestauranteMapper restauranteMapper;
    private final Clock clock;
    private final UsuarioAtual usuarioAtual;

    @Value("${catalogo.horarios.zone-id:America/Recife}")
    private String zoneId;

    public RestauranteService(RestauranteRepo restauranteRepo, HorarioFuncRepo horarioFuncRepo,
                              RestauranteMapper restauranteMapper, Clock clock, UsuarioAtual usuarioAtual) {
        this.restauranteRepo = restauranteRepo;
        this.horarioFuncRepo = horarioFuncRepo;
        this.restauranteMapper = restauranteMapper;
        this.clock = clock;
        this.usuarioAtual = usuarioAtual;
    }

    /** Só contas LOJA cadastram; a conta do token vira a dona do restaurante. */
    public Restaurante cadastrarRestaurante(RestauranteDTO dto) {
        UsuarioAutenticado dono = usuarioAtual.obterLoja();
        validarDados(dto.restauranteNome(), dto.taxaEntrega(), dto.pedidoMinimo(), dto.tempoEstimadoMin(),
                dto.tempoEstimadoMax());
        Restaurante salvo = restauranteRepo.save(restauranteMapper.toEntity(dto, dono.contaId()));
        log.info("[Restaurantes] restaurante {} cadastrado pela conta {}", salvo.getRestauranteId(), dono.contaId());
        return salvo;
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
        validarProprietario(restaurante);
        validarDados(dto.restauranteNome(), dto.taxaEntrega(), dto.pedidoMinimo(), dto.tempoEstimadoMin(),
                dto.tempoEstimadoMax());
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
        restaurante.setPedidoMinimo(dto.pedidoMinimo());
        restaurante.setTempoEstimadoMin(dto.tempoEstimadoMin());
        restaurante.setTempoEstimadoMax(dto.tempoEstimadoMax());
        restaurante.setUrlImagem(dto.urlImagem());
        restaurante.setCategoriaRestaurante(dto.categoriaRestaurante());
        return toResponse(restauranteRepo.save(restaurante));
    }

    @Transactional
    public RestauranteResponseDTO atualizarStatus(Long id, AtualizarStatusRestauranteDTO dto) {
        Restaurante restaurante = buscarEntidade(id);
        validarProprietario(restaurante);
        if (dto.statusRestaurante() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe statusRestaurante.");
        }
        restaurante.setStatusRestaurante(dto.statusRestaurante());
        return toResponse(restauranteRepo.save(restaurante));
    }

    public List<HorarioFuncionamentoDTO> listarHorarios(Long id) {
        buscarEntidade(id);
        return naOrdemDaSemana(horarioFuncRepo.findAllByIdRestauranteRestauranteId(id));
    }

    /** Funcionamento atual para o Checkout: status, se está aberto agora e os horários de hoje. */
    public FuncionamentoRestauranteDTO buscarFuncionamento(Long idRestaurante) {
        Restaurante restaurante = buscarEntidade(idRestaurante);
        List<HorarioFuncionamento> horarios = horarioFuncRepo
                .findAllByIdRestauranteRestauranteId(idRestaurante);
        // Um único instante para abertoAgora e para o dia: perto da meia-noite os dois não divergem.
        Momento agora = agora();
        boolean abertoAgora = estaAberto(restaurante, horarios, agora);
        List<HorarioFuncionamentoDTO> horariosHoje = horarios.stream()
                .filter(h -> h.getDiaSemana() == agora.dia())
                .map(h -> new HorarioFuncionamentoDTO(h.getDiaSemana(), h.getHoraAbertura(), h.getHoraFechamento()))
                .toList();
        log.info("[Restaurantes] Checkout consultou o funcionamento do restaurante {}: status={}, abertoAgora={}, "
                + "pedidoMinimo={}", idRestaurante, restaurante.getStatusRestaurante(), abertoAgora,
                restaurante.getPedidoMinimo());
        return new FuncionamentoRestauranteDTO(restaurante.getStatusRestaurante(), abertoAgora, agora.dia(),
                horariosHoje, restaurante.getPedidoMinimo());
    }

    @Transactional
    public List<HorarioFuncionamentoDTO> substituirHorarios(Long id, SubstituirHorariosDTO dto) {
        Restaurante restaurante = buscarEntidade(id);
        validarProprietario(restaurante);
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
        return naOrdemDaSemana(horarioFuncRepo.saveAll(entidades));
    }

    /** Resposta das rotas de horários: só a lista devolvida é ordenada; o que foi gravado não muda. */
    private static List<HorarioFuncionamentoDTO> naOrdemDaSemana(List<HorarioFuncionamento> horarios) {
        return horarios.stream()
                .sorted(ORDEM_DA_SEMANA)
                .map(h -> new HorarioFuncionamentoDTO(h.getDiaSemana(), h.getHoraAbertura(), h.getHoraFechamento()))
                .toList();
    }

    @Transactional
    public void deletarRestaurante(Long id) {
        Restaurante restaurante = buscarEntidade(id);
        validarProprietario(restaurante);
        restaurante.setStatusRestaurante(StatusRestaurante.INDISPONIVEL);
        restauranteRepo.save(restaurante);
    }

    private Restaurante buscarEntidade(Long id) {
        return restauranteRepo.findById(id).orElseThrow(() -> new RestauranteNaoEncontradoException(id));
    }

    /** A conta do token precisa ser de loja e dona do restaurante; senão, 403. */
    private void validarProprietario(Restaurante restaurante) {
        String conta = usuarioAtual.obterLoja().contaId();
        if (!conta.equalsIgnoreCase(restaurante.getOwnerUserId())) {
            throw new AcessoNegadoException("Usuário não é proprietário do restaurante.");
        }
    }

    private void validarDados(String nome, Long taxa, Long pedidoMinimo, Integer tempoMin, Integer tempoMax) {
        if (nome == null || nome.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe restauranteNome.");
        }
        if (taxa == null || taxa < 0 || tempoMin != null && tempoMin < 0 || tempoMax != null && tempoMax < 0
                || tempoMin != null && tempoMax != null && tempoMin > tempoMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taxa ou tempo estimado inválido.");
        }
        if (pedidoMinimo != null && pedidoMinimo < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pedido mínimo não pode ser negativo.");
        }
    }

    private RestauranteResponseDTO toResponse(Restaurante restaurante) {
        return toResponse(restaurante, horarioFuncRepo
                .findAllByIdRestauranteRestauranteId(restaurante.getRestauranteId()));
    }

    private RestauranteResponseDTO toResponse(Restaurante restaurante, List<HorarioFuncionamento> horarios) {
        RestauranteResponseDTO dto = restauranteMapper.toResponseDTO(restaurante);
        boolean abertoAgora = estaAberto(restaurante, horarios, agora());
        return new RestauranteResponseDTO(dto.restauranteId(), dto.restauranteNome(), dto.restauranteDesc(),
                dto.logradouro(), dto.numero(), dto.complemento(), dto.bairro(), dto.cidade(), dto.estado(), dto.cep(),
                dto.restauranteTelefone(), dto.urlImagem(), dto.ownerUserId(), dto.statusRestaurante(),
                dto.categoriaRestaurante(), dto.taxaEntrega(), dto.pedidoMinimo(), dto.tempoEstimadoMin(),
                dto.tempoEstimadoMax(), abertoAgora);
    }

    /** Aberto agora: status ABERTO e o instante dentro de um dos horários do dia. */
    private static boolean estaAberto(Restaurante restaurante, List<HorarioFuncionamento> horarios, Momento agora) {
        return restaurante.getStatusRestaurante() == StatusRestaurante.ABERTO
                && horarios.stream().anyMatch(h -> h.getDiaSemana() == agora.dia()
                && !agora.hora().isBefore(h.getHoraAbertura()) && agora.hora().isBefore(h.getHoraFechamento()));
    }

    /** Dia e hora atuais no fuso dos horários, lidos do relógio uma única vez. */
    private Momento agora() {
        ZonedDateTime agora = ZonedDateTime.now(clock.withZone(ZoneId.of(zoneId == null ? "America/Recife" : zoneId)));
        return new Momento(diaSemana(agora.getDayOfWeek()), agora.toLocalTime());
    }

    private static DiaSemana diaSemana(DayOfWeek dia) {
        return switch (dia) {
            case SUNDAY -> DiaSemana.DOMINGO;
            case MONDAY -> DiaSemana.SEGUNDA;
            case TUESDAY -> DiaSemana.TERCA;
            case WEDNESDAY -> DiaSemana.QUARTA;
            case THURSDAY -> DiaSemana.QUINTA;
            case FRIDAY -> DiaSemana.SEXTA;
            case SATURDAY -> DiaSemana.SABADO;
        };
    }

    private record Momento(DiaSemana dia, LocalTime hora) {
    }
}
