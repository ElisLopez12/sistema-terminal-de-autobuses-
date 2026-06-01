package com.elis_lopez.terminalAutobusesBackend.service;

import com.elis_lopez.terminalAutobusesBackend.dto.request.SalidaRequest;
import com.elis_lopez.terminalAutobusesBackend.exception.ResourceNotFoundException;
import com.elis_lopez.terminalAutobusesBackend.model.*;
import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import com.elis_lopez.terminalAutobusesBackend.repository.AutobusRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.HorarioRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.RutaRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.SalidaRepository;
import com.elis_lopez.terminalAutobusesBackend.repository.TerminalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio {@link SalidaService}.
 * <p>
 * Cubre los métodos nuevos: {@code asignarAutobus} y {@code ajustarRetrasoConPropagacion}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalidaService — Pruebas unitarias")
class SalidaServiceTest {

    @Mock
    private SalidaRepository salidaRepository;
    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private HorarioRepository horarioRepository;
    @Mock
    private TerminalRepository terminalRepository;
    @Mock
    private AutobusRepository autobusRepository;

    @InjectMocks
    private SalidaService salidaService;

    @Captor
    private ArgumentCaptor<Salida> salidaCaptor;

    private Terminal terminalOrigen;
    private Terminal otroTerminal;
    private Ruta ruta;
    private Salida salida;
    private Autobus autobusDisponible;
    private Autobus autobusOtroTerminal;
    private Autobus autobusSinChofer;
    private Autobus autobusInactivo;

    @BeforeEach
    void setUp() {
        terminalOrigen = new Terminal();
        terminalOrigen.setId(1L);
        terminalOrigen.setNombre("Terminal Maracay");

        otroTerminal = new Terminal();
        otroTerminal.setId(2L);
        otroTerminal.setNombre("Terminal Valencia");

        ruta = new Ruta();
        ruta.setId(10L);
        ruta.setNombre("Maracay → Valencia");
        ruta.setOrigen(terminalOrigen);

        salida = new Salida();
        salida.setId(100L);
        salida.setRuta(ruta);
        salida.setTerminalOrigen(terminalOrigen);
        salida.setHoraProgramada(LocalDateTime.of(2026, 5, 30, 8, 0));
        salida.setRetrasoMinutos(0);
        salida.setEstado(EstadoSalida.PROGRAMADA);
        salida.setActivo(true);

        autobusDisponible = new Autobus();
        autobusDisponible.setId(50L);
        autobusDisponible.setNumeroUnidad("101");
        autobusDisponible.setTerminal(terminalOrigen);
        autobusDisponible.setChofer(new Chofer());
        autobusDisponible.setActivo(true);

        autobusOtroTerminal = new Autobus();
        autobusOtroTerminal.setId(51L);
        autobusOtroTerminal.setNumeroUnidad("102");
        autobusOtroTerminal.setTerminal(otroTerminal);
        autobusOtroTerminal.setChofer(new Chofer());
        autobusOtroTerminal.setActivo(true);

        autobusSinChofer = new Autobus();
        autobusSinChofer.setId(52L);
        autobusSinChofer.setNumeroUnidad("103");
        autobusSinChofer.setTerminal(terminalOrigen);
        autobusSinChofer.setChofer(null);
        autobusSinChofer.setActivo(true);

        autobusInactivo = new Autobus();
        autobusInactivo.setId(53L);
        autobusInactivo.setNumeroUnidad("104");
        autobusInactivo.setTerminal(terminalOrigen);
        autobusInactivo.setChofer(new Chofer());
        autobusInactivo.setActivo(false);
    }

    // ════════════════════════════════════════════════════════════
    // asignarAutobus
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("asignarAutobus — 5 escenarios")
    class AsignarAutobusTests {

        @Test
        @DisplayName("Asignación exitosa — autobús disponible, mismo terminal, con chofer")
        void asignarAutobus_success() {
            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));
            when(autobusRepository.findById(50L)).thenReturn(Optional.of(autobusDisponible));
            when(salidaRepository.countConflictByAutobusAndFecha(
                    eq(50L), anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(100L)))
                    .thenReturn(0L);
            when(salidaRepository.save(any(Salida.class))).thenAnswer(i -> i.getArgument(0));

            var response = salidaService.asignarAutobus(100L, 50L, false);

            verify(salidaRepository).save(salidaCaptor.capture());
            Salida saved = salidaCaptor.getValue();
            assertThat(saved.getAutobus()).isEqualTo(autobusDisponible);
            assertThat(response.getAutobusId()).isEqualTo(50L);
            assertThat(response.getAutobusNumeroUnidad()).isEqualTo("101");
            assertThat(response.isTieneAutobus()).isTrue();
        }

        @Test
        @DisplayName("Autobús de otro terminal — lanza IllegalArgumentException")
        void asignarAutobus_otroTerminal_throws() {
            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));
            when(autobusRepository.findById(51L)).thenReturn(Optional.of(autobusOtroTerminal));

            assertThatThrownBy(() -> salidaService.asignarAutobus(100L, 51L, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("terminal");
        }

        @Test
        @DisplayName("Autobús sin chofer — lanza IllegalArgumentException")
        void asignarAutobus_sinChofer_throws() {
            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));
            when(autobusRepository.findById(52L)).thenReturn(Optional.of(autobusSinChofer));

            assertThatThrownBy(() -> salidaService.asignarAutobus(100L, 52L, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chofer");
        }

        @Test
        @DisplayName("Autobús inactivo — lanza IllegalStateException")
        void asignarAutobus_inactivo_throws() {
            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));
            when(autobusRepository.findById(53L)).thenReturn(Optional.of(autobusInactivo));

            assertThatThrownBy(() -> salidaService.asignarAutobus(100L, 53L, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está activo");
        }

        @Test
        @DisplayName("Conflicto horario — autobús tiene otra salida el mismo día — lanza IllegalStateException")
        void asignarAutobus_conflictoHorario_throws() {
            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));
            when(autobusRepository.findById(50L)).thenReturn(Optional.of(autobusDisponible));
            when(salidaRepository.countConflictByAutobusAndFecha(
                    eq(50L), anyList(), any(LocalDateTime.class), any(LocalDateTime.class), eq(100L)))
                    .thenReturn(1L);

            assertThatThrownBy(() -> salidaService.asignarAutobus(100L, 50L, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ocupado");
        }

        @Test
        @DisplayName("Salida no encontrada — lanza ResourceNotFoundException")
        void asignarAutobus_salidaNoExiste_throws() {
            when(salidaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> salidaService.asignarAutobus(999L, 50L, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Salida no está PROGRAMADA — lanza IllegalStateException")
        void asignarAutobus_salidaNoProgramada_throws() {
            salida.setEstado(EstadoSalida.EN_RUTA);
            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));

            assertThatThrownBy(() -> salidaService.asignarAutobus(100L, 50L, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PROGRAMADA");
        }
    }

    // ════════════════════════════════════════════════════════════
    // ajustarRetrasoConPropagacion
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ajustarRetrasoConPropagacion — 4 escenarios")
    class AjustarRetrasoTests {

        private Salida s1, s2, s3, s4;

        @BeforeEach
        void setUp() {
            s1 = new Salida();
            s1.setId(1L);
            s1.setRuta(ruta);
            s1.setTerminalOrigen(terminalOrigen);
            s1.setHoraProgramada(LocalDateTime.of(2026, 5, 30, 8, 0));
            s1.setRetrasoMinutos(0);
            s1.setEstado(EstadoSalida.PROGRAMADA);

            s2 = new Salida();
            s2.setId(2L);
            s2.setRuta(ruta);
            s2.setTerminalOrigen(terminalOrigen);
            s2.setHoraProgramada(LocalDateTime.of(2026, 5, 30, 9, 0));
            s2.setRetrasoMinutos(0);
            s2.setEstado(EstadoSalida.PROGRAMADA);

            s3 = new Salida();
            s3.setId(3L);
            s3.setRuta(ruta);
            s3.setTerminalOrigen(terminalOrigen);
            s3.setHoraProgramada(LocalDateTime.of(2026, 5, 30, 10, 0));
            s3.setRetrasoMinutos(0);
            s3.setEstado(EstadoSalida.PROGRAMADA);

            s4 = new Salida();
            s4.setId(4L);
            s4.setRuta(ruta);
            s4.setTerminalOrigen(terminalOrigen);
            s4.setHoraProgramada(LocalDateTime.of(2026, 5, 30, 11, 0));
            s4.setRetrasoMinutos(0);
            s4.setEstado(EstadoSalida.PROGRAMADA);
        }

        @Test
        @DisplayName("Propagación a tres salidas siguientes — todas PROGRAMADAs se desplazan")
        void ajustarRetraso_propagaATresSalidas() {
            when(salidaRepository.findById(1L)).thenReturn(Optional.of(s1));
            when(salidaRepository.findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc(
                    eq(10L), eq(LocalDateTime.of(2026, 5, 30, 8, 0))))
                    .thenReturn(List.of(s2, s3, s4));

            var response = salidaService.ajustarRetrasoConPropagacion(1L, 30);

            assertThat(s1.getRetrasoMinutos()).isEqualTo(30);
            assertThat(s2.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 9, 30));
            assertThat(s3.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 10, 30));
            assertThat(s4.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 11, 30));
            assertThat(response.getRetrasoMinutos()).isEqualTo(30);

            verify(salidaRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("No propaga a salidas EN_RUTA o CANCELADAS — solo salta esos estados")
        void ajustarRetraso_noPropagaAEnCurso() {
            s2.setEstado(EstadoSalida.EN_RUTA);
            s4.setEstado(EstadoSalida.CANCELADA);

            when(salidaRepository.findById(1L)).thenReturn(Optional.of(s1));
            when(salidaRepository.findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc(
                    eq(10L), eq(LocalDateTime.of(2026, 5, 30, 8, 0))))
                    .thenReturn(List.of(s2, s3, s4));

            salidaService.ajustarRetrasoConPropagacion(1L, 15);

            assertThat(s2.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 9, 0));
            assertThat(s3.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 10, 15));
            assertThat(s4.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 11, 0));
        }

        @Test
        @DisplayName("Retraso en salida intermedia solo propaga hacia adelante — salidas anteriores no se modifican")
        void ajustarRetraso_soloPropagaAdelante() {
            when(salidaRepository.findById(2L)).thenReturn(Optional.of(s2));
            when(salidaRepository.findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc(
                    eq(10L), eq(LocalDateTime.of(2026, 5, 30, 9, 0))))
                    .thenReturn(List.of(s3));

            salidaService.ajustarRetrasoConPropagacion(2L, 20);

            assertThat(s1.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 8, 0));
            assertThat(s3.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 10, 20));
        }

        @Test
        @DisplayName("Retraso en cero no modifica nada — horaProgramada permanece igual")
        void ajustarRetraso_cero_noModificaNada() {
            when(salidaRepository.findById(1L)).thenReturn(Optional.of(s1));
            // Cuando retraso = 0, el scheduler no busca salidas siguientes (diferencia = 0, no propaga)
            // por lo que la llamada a findByRutaId... nunca ocurre

            salidaService.ajustarRetrasoConPropagacion(1L, 0);

            assertThat(s1.getRetrasoMinutos()).isEqualTo(0);
            assertThat(s2.getHoraProgramada()).isEqualTo(LocalDateTime.of(2026, 5, 30, 9, 0));

            verify(salidaRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Salida no encontrada — lanza ResourceNotFoundException")
        void ajustarRetraso_salidaNoExiste_throws() {
            when(salidaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> salidaService.ajustarRetrasoConPropagacion(999L, 15))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════
    // create — guarda contra autobús sin chofer
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create — guarda contra autobús sin chofer")
    class CreateTests {

        private SalidaRequest buildRequest(Long autobusId) {
            SalidaRequest req = new SalidaRequest();
            req.setRutaId(10L);
            req.setTerminalOrigenId(1L);
            req.setAutobusId(autobusId);
            req.setHoraProgramada(LocalDateTime.of(2026, 6, 1, 8, 0));
            return req;
        }

        @Test
        @DisplayName("Autobús sin chofer — lanza IllegalArgumentException")
        void create_sinChofer_throws() {
            SalidaRequest request = buildRequest(52L);

            when(rutaRepository.findById(10L)).thenReturn(Optional.of(ruta));
            when(terminalRepository.findById(1L)).thenReturn(Optional.of(terminalOrigen));
            when(autobusRepository.findById(52L)).thenReturn(Optional.of(autobusSinChofer));

            assertThatThrownBy(() -> salidaService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chofer");
        }

        @Test
        @DisplayName("Autobús con chofer — creación exitosa")
        void create_conChofer_success() {
            SalidaRequest request = buildRequest(50L);

            when(rutaRepository.findById(10L)).thenReturn(Optional.of(ruta));
            when(terminalRepository.findById(1L)).thenReturn(Optional.of(terminalOrigen));
            when(autobusRepository.findById(50L)).thenReturn(Optional.of(autobusDisponible));
            when(salidaRepository.save(any(Salida.class))).thenAnswer(i -> i.getArgument(0));

            var response = salidaService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getAutobusId()).isEqualTo(50L);
        }
    }

    // ════════════════════════════════════════════════════════════
    // update — guarda contra autobús sin chofer
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update — guarda contra autobús sin chofer")
    class UpdateTests {

        private SalidaRequest buildRequest(Long autobusId) {
            SalidaRequest req = new SalidaRequest();
            req.setRutaId(10L);
            req.setTerminalOrigenId(1L);
            req.setAutobusId(autobusId);
            req.setHoraProgramada(LocalDateTime.of(2026, 6, 1, 8, 0));
            return req;
        }

        @Test
        @DisplayName("Autobús sin chofer en update — lanza IllegalArgumentException")
        void update_sinChofer_throws() {
            SalidaRequest request = buildRequest(52L);

            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));
            when(rutaRepository.findById(10L)).thenReturn(Optional.of(ruta));
            when(terminalRepository.findById(1L)).thenReturn(Optional.of(terminalOrigen));
            when(autobusRepository.findById(52L)).thenReturn(Optional.of(autobusSinChofer));

            assertThatThrownBy(() -> salidaService.update(100L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chofer");
        }

        @Test
        @DisplayName("Autobús con chofer en update — actualización exitosa")
        void update_conChofer_success() {
            SalidaRequest request = buildRequest(50L);

            when(salidaRepository.findById(100L)).thenReturn(Optional.of(salida));
            when(rutaRepository.findById(10L)).thenReturn(Optional.of(ruta));
            when(terminalRepository.findById(1L)).thenReturn(Optional.of(terminalOrigen));
            when(autobusRepository.findById(50L)).thenReturn(Optional.of(autobusDisponible));
            when(salidaRepository.save(any(Salida.class))).thenAnswer(i -> i.getArgument(0));

            var response = salidaService.update(100L, request);

            assertThat(response).isNotNull();
            assertThat(response.getAutobusId()).isEqualTo(50L);
        }
    }
}
