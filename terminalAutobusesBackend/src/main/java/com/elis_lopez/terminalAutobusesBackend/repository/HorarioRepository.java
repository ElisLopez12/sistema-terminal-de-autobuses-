package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.Horario;
import com.elis_lopez.terminalAutobusesBackend.model.enums.DiaSemana;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Horario}.
 */
@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findByRutaId(Long rutaId);

    Page<Horario> findByRutaId(Long rutaId, Pageable pageable);

    List<Horario> findByTerminalOrigenId(Long terminalId);

    Page<Horario> findByTerminalOrigenId(Long terminalId, Pageable pageable);

    /**
     * Busca horarios activos cuyo día de la semana coincida con el dado.
     * <p>
     * Utilizado por el scheduler de generación automática de salidas
     * para encontrar qué horarios aplicar hoy.
     *
     * @param dia día de la semana a filtrar
     * @return lista de horarios activos para ese día
     */
    List<Horario> findByActivoTrueAndDiaSemanaIn(List<DiaSemana> dias);
}
