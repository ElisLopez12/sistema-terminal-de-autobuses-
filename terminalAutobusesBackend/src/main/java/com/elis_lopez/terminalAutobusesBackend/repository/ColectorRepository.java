package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.Colector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Colector}.
 */
@Repository
public interface ColectorRepository extends JpaRepository<Colector, Long> {

    List<Colector> findByTerminalId(Long terminalId);

    Page<Colector> findByTerminalId(Long terminalId, Pageable pageable);
}
