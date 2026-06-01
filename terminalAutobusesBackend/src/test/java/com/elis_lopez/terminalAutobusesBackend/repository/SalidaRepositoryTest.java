package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.*;
import com.elis_lopez.terminalAutobusesBackend.model.enums.DiaSemana;
import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para las queries personalizadas del repositorio {@link SalidaRepository}.
 * <p>
 * Verifica que {@code countByHorarioIdAndFecha} y {@code findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc}
 * funcionen correctamente contra H2 en memoria.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SalidaRepository — Queries personalizadas")
class SalidaRepositoryTest {

    @Autowired
    private SalidaRepository salidaRepository;

    @Autowired
    private EntityManager em;

    /**
     * Persiste una entidad y hace flush para que esté disponible en la BD.
     */
    private void persist(Object entity) {
        em.persist(entity);
        em.flush();
    }

    private Horario horario;
    private Ruta ruta;
    private Terminal terminal;
    private Salida salida1;
    private Salida salida2;

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
        terminal.setNombre("Terminal Test");
        terminal.setActivo(true);
        persist(terminal);

        ruta = new Ruta();
        ruta.setNombre("Test → Destino");
        ruta.setOrigen(terminal);
        ruta.setDestinoNombre("Destino");
        ruta.setActivo(true);
        persist(ruta);

        horario = new Horario();
        horario.setRuta(ruta);
        horario.setTerminalOrigen(terminal);
        horario.setDiaSemana(DiaSemana.LUNES);
        horario.setHoraInicio(LocalTime.of(6, 0));
        horario.setHoraFin(LocalTime.of(8, 0));
        horario.setIntervaloMinutos(60);
        horario.setActivo(true);
        persist(horario);

        salida1 = new Salida();
        salida1.setRuta(ruta);
        salida1.setHorario(horario);
        salida1.setTerminalOrigen(terminal);
        salida1.setHoraProgramada(LocalDateTime.of(2026, 5, 30, 6, 0));
        salida1.setRetrasoMinutos(0);
        salida1.setEstado(EstadoSalida.PROGRAMADA);
        salida1.setActivo(true);
        persist(salida1);

        salida2 = new Salida();
        salida2.setRuta(ruta);
        salida2.setHorario(horario);
        salida2.setTerminalOrigen(terminal);
        salida2.setHoraProgramada(LocalDateTime.of(2026, 5, 30, 7, 0));
        salida2.setRetrasoMinutos(0);
        salida2.setEstado(EstadoSalida.PROGRAMADA);
        salida2.setActivo(true);
        persist(salida2);
    }

    @Test
    @DisplayName("countByHorarioIdAndFecha — retorna conteo correcto para horario+fecha")
    void countByHorarioIdAndFecha_returnsCorrectCount() {
        LocalDateTime inicio = LocalDateTime.of(2026, 5, 30, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 5, 31, 0, 0);
        long count = salidaRepository.countByHorarioIdAndFecha(horario.getId(), inicio, fin);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("countByHorarioIdAndFecha — retorna 0 para fecha sin salidas")
    void countByHorarioIdAndFecha_returnsZeroForNoSalidas() {
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 2, 0, 0);
        long count = salidaRepository.countByHorarioIdAndFecha(horario.getId(), inicio, fin);

        assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("findByRutaIdAndHoraProgramadaAfter — retorna salidas posteriores ordenadas")
    void findByRutaIdAndHoraProgramadaAfter_returnsOrderedSalidas() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 5, 30, 6, 30);

        List<Salida> result = salidaRepository
                .findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc(ruta.getId(), cutoff);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(salida2.getId());
        assertThat(result.get(0).getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 7, 0));
    }

    @Test
    @DisplayName("findByRutaIdAndHoraProgramadaAfter — retorna lista vacía si no hay salidas posteriores")
    void findByRutaIdAndHoraProgramadaAfter_returnsEmptyWhenNoneAfter() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 5, 30, 23, 0);

        List<Salida> result = salidaRepository
                .findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc(ruta.getId(), cutoff);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("countConflictByAutobusAndFecha — retorna 0 si no hay conflictos")
    void countConflictByAutobusAndFecha_returnsZeroWhenNoConflict() {
        Autobus autobus = new Autobus();
        autobus.setNumeroUnidad("200");
        autobus.setMatricula("CONF-01");
        autobus.setTerminal(terminal);
        autobus.setActivo(true);
        persist(autobus);

        long count = salidaRepository.countConflictByAutobusAndFecha(
                autobus.getId(),
                List.of(EstadoSalida.PROGRAMADA, EstadoSalida.ABORDAJE, EstadoSalida.EN_RUTA),
                LocalDateTime.of(2026, 5, 30, 0, 0),
                LocalDateTime.of(2026, 5, 31, 0, 0),
                0L);

        assertThat(count).isEqualTo(0L);
    }
}
