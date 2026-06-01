package com.elis_lopez.terminalAutobusesBackend.scheduler;

import com.elis_lopez.terminalAutobusesBackend.model.Horario;
import com.elis_lopez.terminalAutobusesBackend.model.Salida;
import com.elis_lopez.terminalAutobusesBackend.model.enums.DiaSemana;
import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import com.elis_lopez.terminalAutobusesBackend.repository.SalidaRepository;
import com.elis_lopez.terminalAutobusesBackend.service.HorarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler que genera automáticamente las salidas del día a partir
 * de los horarios activos.
 * <p>
 * Se ejecuta diariamente a las 05:00 (America/Caracas). También puede
 * ser invocado manualmente mediante el endpoint {@code POST /api/v1/salidas/generar-del-dia}.
 * <p>
 * Por cada horario activo cuyo {@code diaSemana} coincida con el día actual,
 * genera salidas desde {@code horaInicio} hasta {@code horaFin} con paso
 * {@code intervaloMinutos}. Verifica idempotencia consultando si ya existen
 * salidas para ese horario+fecha.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SalidaScheduler {

    private final HorarioService horarioService;
    private final SalidaRepository salidaRepository;

    /**
     * Ejecución programada diaria a las 05:00 (America/Caracas).
     * <p>
     * Delega en {@link #generarSalidasDelDia()} que contiene la lógica
     * compartida con la invocación manual.
     */
    @Scheduled(cron = "0 0 5 * * *", zone = "America/Caracas")
    public void ejecucionProgramada() {
        log.info("Iniciando generación programada de salidas del día");
        Map<String, Integer> resultado = generarSalidasDelDia();
        log.info("Generación programada completada: {}", resultado);
    }

    /**
     * Genera las salidas del día actual a partir de horarios activos.
     * <p>
     * Lógica compartida entre la ejecución programada y la invocación manual:
     * <ol>
     *   <li>Obtiene los horarios activos para el día de la semana actual</li>
     *   <li>Para cada horario, verifica que {@code terminalOrigen} coincida con {@code ruta.origen}</li>
     *   <li>Verifica idempotencia: si ya existen salidas para ese horario+fecha, salta</li>
     *   <li>Genera salidas desde {@code horaInicio} hasta {@code horaFin} con paso {@code intervaloMinutos}</li>
     * </ol>
     *
     * @return mapa con {@code salidasGeneradas}, {@code horariosProcesados}, {@code horariosSaltados}
     */
    @Transactional
    public Map<String, Integer> generarSalidasDelDia() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Caracas"));
        DiaSemana diaSemana = fromDayOfWeek(hoy.getDayOfWeek());

        List<Horario> horariosActivos = horarioService.findActivosByDia(diaSemana);
        log.info("Generando salidas para {} ({} horarios activos)", diaSemana, horariosActivos.size());

        int salidasGeneradas = 0;
        int horariosProcesados = 0;
        int horariosSaltados = 0;

        for (Horario horario : horariosActivos) {
            // Validar que terminalOrigen coincida con ruta.origen
            if (!horario.getTerminalOrigen().getId().equals(horario.getRuta().getOrigen().getId())) {
                log.error("Horario {} saltado: terminalOrigen.id {} != ruta.origen.id {}",
                        horario.getId(),
                        horario.getTerminalOrigen().getId(),
                        horario.getRuta().getOrigen().getId());
                horariosSaltados++;
                continue;
            }

            // Idempotencia: verificar si ya existen salidas para este horario+fecha
            LocalDateTime inicioDia = hoy.atStartOfDay();
            LocalDateTime finDia = hoy.plusDays(1).atStartOfDay();
            long existentes = salidaRepository.countByHorarioIdAndFecha(horario.getId(), inicioDia, finDia);
            if (existentes > 0) {
                log.info("Horario {} saltado: ya existen {} salidas para hoy", horario.getId(), existentes);
                horariosSaltados++;
                continue;
            }

            // Generar salidas desde horaInicio hasta horaFin, saltando horas ya pasadas
            List<Salida> salidas = new ArrayList<>();
            LocalTime horaActual = horario.getHoraInicio();

            while (!horaActual.isAfter(horario.getHoraFin())) {
                LocalDateTime horaProgramada = LocalDateTime.of(hoy, horaActual);
                // Saltar horarios que ya pasaron (solo para generación manual)
                if (horaProgramada.isBefore(LocalDateTime.now(ZoneId.of("America/Caracas")))) {
                    horaActual = horaActual.plusMinutes(horario.getIntervaloMinutos());
                    continue;
                }

                Salida salida = new Salida();
                salida.setRuta(horario.getRuta());
                salida.setHorario(horario);
                salida.setTerminalOrigen(horario.getTerminalOrigen());
                salida.setHoraProgramada(horaProgramada);
                salida.setRetrasoMinutos(0);
                salida.setEstado(EstadoSalida.PROGRAMADA);
                salida.setActivo(true);
                salidas.add(salida);

                horaActual = horaActual.plusMinutes(horario.getIntervaloMinutos());
            }

            if (!salidas.isEmpty()) {
                salidaRepository.saveAll(salidas);
                salidasGeneradas += salidas.size();
                horariosProcesados++;
                log.info("Generadas {} salidas para horario {}", salidas.size(), horario.getId());
            }
        }

        Map<String, Integer> resultado = new HashMap<>();
        resultado.put("salidasGeneradas", salidasGeneradas);
        resultado.put("horariosProcesados", horariosProcesados);
        resultado.put("horariosSaltados", horariosSaltados);

        log.info("Generación completada: {} salidas, {} horarios procesados, {} saltados",
                salidasGeneradas, horariosProcesados, horariosSaltados);
        return resultado;
    }

    /**
     * Limpia salidas PROGRAMADA sin autobús cuya hora programada ya pasó.
     * <p>
     * Se ejecuta cada 30 minutos. Marca como inactivas (soft delete) aquellas
     * salidas que nunca recibieron un autobús y ya expiraron, para no saturar
     * la vista de salidas con registros huérfanos.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "America/Caracas")
    @Transactional
    public void limpiarSalidasExpiradasSinAutobus() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Caracas"));
        List<Salida> expiradas = salidaRepository.findExpiredSinAutobus(EstadoSalida.PROGRAMADA, ahora);

        if (expiradas.isEmpty()) {
            return;
        }

        log.info("Limpiando {} salidas PROGRAMADA sin autobús y con hora vencida", expiradas.size());
        for (Salida salida : expiradas) {
            salida.setActivo(false);
        }
        salidaRepository.saveAll(expiradas);
        log.info("Limpieza completada: {} salidas desactivadas", expiradas.size());
    }

    /**
     * Convierte un {@link DayOfWeek} de Java a la enumeración {@link DiaSemana}.
     * <p>
     * Los días en la BD están en español (LUNES, MARTES, ...),
     * mientras que {@code DayOfWeek} usa inglés (MONDAY, TUESDAY, ...).
     *
     * @param dayOfWeek día de la semana en inglés
     * @return día de la semana en español
     */
    private DiaSemana fromDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }
}
