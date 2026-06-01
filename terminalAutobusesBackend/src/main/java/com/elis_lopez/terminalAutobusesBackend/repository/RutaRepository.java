package com.elis_lopez.terminalAutobusesBackend.repository;

import com.elis_lopez.terminalAutobusesBackend.model.Ruta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Ruta}.
 */
@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {

    List<Ruta> findByOrigenId(Long terminalId);

    Page<Ruta> findByOrigenId(Long terminalId, Pageable pageable);

    Page<Ruta> findByActivoTrue(Pageable pageable);

    Page<Ruta> findByActivoTrueAndOrigenId(Long origenId, Pageable pageable);
}
