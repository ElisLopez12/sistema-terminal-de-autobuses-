package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.Autobus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Autobus}.
 */
@Repository
public interface AutobusRepository extends JpaRepository<Autobus, Long> {

    List<Autobus> findByTerminalId(Long terminalId);

    Page<Autobus> findByTerminalId(Long terminalId, Pageable pageable);

    List<Autobus> findByRutaId(Long rutaId);

    Page<Autobus> findByRutaId(Long rutaId, Pageable pageable);

    Optional<Autobus> findByChoferId(Long choferId);

    Optional<Autobus> findByColectorId(Long colectorId);
}
