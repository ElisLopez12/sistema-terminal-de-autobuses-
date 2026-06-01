package com.elis_lopez.terminalAutobusesBackend.scheduler;

import com.elis_lopez.terminalAutobusesBackend.model.Horario;
import com.elis_lopez.terminalAutobusesBackend.model.Ruta;
import com.elis_lopez.terminalAutobusesBackend.model.Salida;
import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import com.elis_lopez.terminalAutobusesBackend.model.enums.DiaSemana;
import com.elis_lopez.terminalAutobusesBackend.repository.HorarioRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.SalidaRepository;
import com.elis_lopez.terminalAutobusesBackend.service.HorarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del scheduler {@link SalidaScheduler}.
 * <p>
 * Verifica la generación de salidas diarias a partir de horarios activos,
 * idempotencia (no duplicar salidas existentes) y salto de horarios inválidos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalidaScheduler — Generación automática de salidas")
class SalidaSchedulerTest {

    @Mock
    private HorarioService horarioService;

    @Mock
    private SalidaRepository salidaRepository;

    @InjectMocks
    private SalidaScheduler salidaScheduler;

    private Horario horarioLunes;
    private Ruta ruta;
    private Terminal terminal;

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
        terminal.setId(1L);
        terminal.setNombre("Terminal Maracay");

        ruta = new Ruta();
        ruta.setId(10L);
        ruta.setNombre("Maracay → Valencia");
        ruta.setOrigen(terminal);

        horarioLunes = new Horario();
        horarioLunes.setId(100L);
        horarioLunes.setRuta(ruta);
        horarioLunes.setTerminalOrigen(terminal);
        horarioLunes.setDiaSemana(DiaSemana.LUNES);
        horarioLunes.setHoraInicio(LocalTime.of(6, 0));
        horarioLunes.setHoraFin(LocalTime.of(8, 0));
        horarioLunes.setIntervaloMinutos(60);
        horarioLunes.setActivo(true);
    }

    @Test
    @DisplayName("Genera salidas para horarios activos del día — 3 salidas si intervalo 60min en 06:00-08:00 (inclusive)")
    void generarSalidasDelDia_createsSalidasForActiveHorarios() {
        when(horarioService.findActivosByDia(any(DiaSemana.class)))
                .thenReturn(List.of(horarioLunes));
        when(salidaRepository.countByHorarioIdAndFecha(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        var result = salidaScheduler.generarSalidasDelDia();

        assertThat(result).isNotNull();
        assertThat(result.get("salidasGeneradas")).isEqualTo(3);
        assertThat(result.get("horariosProcesados")).isEqualTo(1);
        assertThat(result.get("horariosSaltados")).isEqualTo(0);

        verify(salidaRepository).saveAll(argThat((List<Salida> salidas) -> {
            assertThat(salidas).hasSize(3);
            assertThat(salidas.get(0).getHoraProgramada().toLocalTime()).isEqualTo(LocalTime.of(6, 0));
            assertThat(salidas.get(1).getHoraProgramada().toLocalTime()).isEqualTo(LocalTime.of(7, 0));
            assertThat(salidas.get(2).getHoraProgramada().toLocalTime()).isEqualTo(LocalTime.of(8, 0));
            assertThat(salidas.get(0).getEstado()).isEqualTo(com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida.PROGRAMADA);
            assertThat(salidas.get(0).getAutobus()).isNull();
            assertThat(salidas.get(0).getHorario()).isEqualTo(horarioLunes);
            assertThat(salidas.get(0).getRetrasoMinutos()).isEqualTo(0);
            return true;
        }));
    }

    @Test
    @DisplayName("Idempotencia — no duplica salidas si ya existen para ese horario+fecha")
    void generarSalidasDelDia_skipsHorarioWhenSalidasExist() {
        when(horarioService.findActivosByDia(any(DiaSemana.class)))
                .thenReturn(List.of(horarioLunes));
        when(salidaRepository.countByHorarioIdAndFecha(eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);

        var result = salidaScheduler.generarSalidasDelDia();

        assertThat(result.get("salidasGeneradas")).isEqualTo(0);
        assertThat(result.get("horariosProcesados")).isEqualTo(0);
        assertThat(result.get("horariosSaltados")).isEqualTo(1);

        verify(salidaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Horario con terminalOrigen != ruta.origen se salta sin generar salidas")
    void generarSalidasDelDia_skipsHorarioWhenTerminalMismatch() {
        Terminal otroTerminal = new Terminal();
        otroTerminal.setId(2L);
        otroTerminal.setNombre("Terminal Valencia");
        horarioLunes.setTerminalOrigen(otroTerminal); // mismatch: ruta.origen = terminal 1

        when(horarioService.findActivosByDia(any(DiaSemana.class)))
                .thenReturn(List.of(horarioLunes));

        var result = salidaScheduler.generarSalidasDelDia();

        assertThat(result.get("salidasGeneradas")).isEqualTo(0);
        assertThat(result.get("horariosProcesados")).isEqualTo(0);
        assertThat(result.get("horariosSaltados")).isEqualTo(1);

        verify(salidaRepository, never()).countByHorarioIdAndFecha(anyLong(), any(), any());
        verify(salidaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("No hay horarios activos para el día — resultado vacío")
    void generarSalidasDelDia_noHorarios_returnsEmpty() {
        when(horarioService.findActivosByDia(any(DiaSemana.class)))
                .thenReturn(List.of());

        var result = salidaScheduler.generarSalidasDelDia();

        assertThat(result.get("salidasGeneradas")).isEqualTo(0);
        assertThat(result.get("horariosProcesados")).isEqualTo(0);
        assertThat(result.get("horariosSaltados")).isEqualTo(0);

        verify(salidaRepository, never()).countByHorarioIdAndFecha(anyLong(), any(), any());
        verify(salidaRepository, never()).saveAll(any());
    }
}
