package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.Terminal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad {@link Terminal}.
 */
@Repository
public interface TerminalRepository extends JpaRepository<Terminal, Long> {

    Page<Terminal> findByActivoTrue(Pageable pageable);
}
