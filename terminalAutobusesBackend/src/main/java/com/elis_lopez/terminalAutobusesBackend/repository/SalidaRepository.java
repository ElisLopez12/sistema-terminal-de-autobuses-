package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.Salida;
import com.elis_lopez.terminalAutobusesBackend.model.enums.EstadoSalida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Salida}.
 */
@Repository
public interface SalidaRepository extends JpaRepository<Salida, Long> {

    List<Salida> findByRutaId(Long rutaId);

    Page<Salida> findByRutaId(Long rutaId, Pageable pageable);

    Page<Salida> findByActivoTrue(Pageable pageable);

    List<Salida> findByTerminalOrigenId(Long terminalId);

    Page<Salida> findByTerminalOrigenId(Long terminalId, Pageable pageable);

    List<Salida> findByAutobusId(Long autobusId);

    /**
     * Cuenta las salidas existentes para un horario y fecha específicos.
     * <p>
     * Utilizado por el scheduler para idempotencia: si ya existen salidas
     * para este horario+fecha, no se generan duplicados.
     *
     * @param horarioId ID del horario
     * @param fecha     fecha (sin hora) a verificar
     * @return cantidad de salidas existentes
     */
    @Query("SELECT COUNT(s) FROM Salida s WHERE s.horario.id = :horarioId " +
            "AND s.horaProgramada >= :inicio AND s.horaProgramada < :fin")
    long countByHorarioIdAndFecha(@Param("horarioId") Long horarioId,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fin") LocalDateTime fin);

    /**
     * Busca salidas de una ruta cuya horaProgramada sea posterior a la dada,
     * ordenadas por horaProgramada ascendente.
     * <p>
     * Utilizado para la propagación de retrasos: al ajustar el retraso de una
     * salida, se buscan las siguientes salidas de la misma ruta ese día.
     *
     * @param rutaId ID de la ruta
     * @param hora   hora límite (salidas posteriores a esta)
     * @return lista de salidas ordenadas por horaProgramada
     */
    List<Salida> findByRutaIdAndHoraProgramadaAfterOrderByHoraProgramadaAsc(Long rutaId, LocalDateTime hora);

    /**
     * Cuenta salidas activas de un autobús en un día específico, excluyendo una salida dada.
     * <p>
     * Utilizado para verificar conflictos horarios al asignar un autobús a una salida.
     *
     * @param autobusId ID del autobús
     * @param estados   estados a considerar (PROGRAMADA, ABORDAJE, EN_RUTA)
     * @param inicio    inicio del día
     * @param fin       fin del día
     * @param salidaId  ID de la salida a excluir (para reasignaciones)
     * @return cantidad de salidas en conflicto
     */
    @Query("SELECT COUNT(s) FROM Salida s WHERE s.autobus.id = :autobusId AND s.estado IN :estados " +
            "AND s.horaProgramada >= :inicio AND s.horaProgramada < :fin AND s.id <> :salidaId")
    long countConflictByAutobusAndFecha(@Param("autobusId") Long autobusId,
                                        @Param("estados") List<EstadoSalida> estados,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin,
                                        @Param("salidaId") Long salidaId);

    List<Salida> findByActivoTrueAndEstadoNotOrderByHoraProgramadaAsc(EstadoSalida estado);

    Page<Salida> findByActivoTrueAndEstadoNot(EstadoSalida estado, Pageable pageable);

    List<Salida> findByActivoTrueAndEstadoNotAndRutaIdOrderByHoraProgramadaAsc(EstadoSalida estado, Long rutaId);

    Page<Salida> findByActivoTrueAndEstadoNotAndRutaId(EstadoSalida estado, Long rutaId, Pageable pageable);

    List<Salida> findByActivoTrueAndEstadoNotAndTerminalOrigenIdOrderByHoraProgramadaAsc(EstadoSalida estado, Long terminalId);

    Page<Salida> findByActivoTrueAndEstadoNotAndTerminalOrigenId(EstadoSalida estado, Long terminalId, Pageable pageable);

    // === Queries para endpoints públicos: solo PROGRAMADA y ABORDAJE ===

    /**
     * Busca salidas activas con estado en la lista dada, ordenadas por horaProgramada ascendente.
     * <p>
     * Utilizado por el endpoint público para mostrar solo salidas PROGRAMADA y ABORDAJE.
     *
     * @param estados  lista de estados a incluir
     * @param pageable parámetros de paginación y ordenación
     * @return página de salidas activas con los estados indicados
     */
    Page<Salida> findByActivoTrueAndEstadoInOrderByHoraProgramadaAsc(List<EstadoSalida> estados, Pageable pageable);

    /**
     * Busca salidas activas de una ruta con estado en la lista dada, ordenadas por horaProgramada.
     *
     * @param estados  lista de estados a incluir
     * @param rutaId   ID de la ruta
     * @param pageable parámetros de paginación
     * @return página de salidas activas de la ruta con los estados indicados
     */
    Page<Salida> findByActivoTrueAndEstadoInAndRutaIdOrderByHoraProgramadaAsc(List<EstadoSalida> estados, Long rutaId, Pageable pageable);

    /**
     * Busca salidas activas de un terminal con estado en la lista dada, ordenadas por horaProgramada.
     *
     * @param estados    lista de estados a incluir
     * @param terminalId ID del terminal de origen
     * @param pageable   parámetros de paginación
     * @return página de salidas activas del terminal con los estados indicados
     */
    Page<Salida> findByActivoTrueAndEstadoInAndTerminalOrigenIdOrderByHoraProgramadaAsc(List<EstadoSalida> estados, Long terminalId, Pageable pageable);

    /**
     * Busca salidas activas en estado PROGRAMADA, sin autobús asignado,
     * cuya hora programada ya haya pasado.
     * <p>
     * Utilizado por el scheduler para limpiar salidas huérfanas que nunca
     * recibieron un autobús y ya no tienen sentido.
     *
     * @param estado estado a buscar (PROGRAMADA)
     * @param ahora  momento actual; se buscan salidas con horaProgramada anterior
     * @return lista de salidas expiradas
     */
    @Query("SELECT s FROM Salida s WHERE s.activo = true AND s.estado = :estado AND s.autobus IS NULL AND s.horaProgramada < :ahora")
    List<Salida> findExpiredSinAutobus(@Param("estado") EstadoSalida estado,
                                       @Param("ahora") LocalDateTime ahora);
}
