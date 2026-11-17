package br.upe.catalogosdelivery.restaurantes.entities;

import br.upe.catalogosdelivery.restaurantes.entities.Enums.DiaSemana;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
class HorarioFuncionamentoJsonTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void serializaHorarioEInstanteEmIso8601() throws Exception {
        HorarioFuncionamento horario = new HorarioFuncionamento();
        horario.setDiaSemana(DiaSemana.SEGUNDA);
        horario.setHoraAbertura(LocalTime.of(9, 0));
        horario.setHoraFechamento(LocalTime.of(18, 30));

        String json = jsonMapper.writeValueAsString(horario);

        assertTrue(json.contains("\"horaAbertura\":\"09:00:00\""));
        assertTrue(json.contains("\"horaFechamento\":\"18:30:00\""));
        assertTrue(json.contains("\"diaSemana\":\"SEGUNDA\""));
        assertEquals("\"2026-08-24T14:30:00Z\"",
                jsonMapper.writeValueAsString(Instant.parse("2026-08-24T14:30:00Z")));

        HorarioFuncionamento restaurado = jsonMapper.readValue(json, HorarioFuncionamento.class);
        assertEquals(horario.getHoraAbertura(), restaurado.getHoraAbertura());
        assertEquals(horario.getHoraFechamento(), restaurado.getHoraFechamento());
    }
}
