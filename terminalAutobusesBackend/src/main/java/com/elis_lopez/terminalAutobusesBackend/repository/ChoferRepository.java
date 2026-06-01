package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.Chofer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Chofer}.
 */
@Repository
public interface ChoferRepository extends JpaRepository<Chofer, Long> {

    List<Chofer> findByTerminalId(Long terminalId);

    Page<Chofer> findByTerminalId(Long terminalId, Pageable pageable);
}
