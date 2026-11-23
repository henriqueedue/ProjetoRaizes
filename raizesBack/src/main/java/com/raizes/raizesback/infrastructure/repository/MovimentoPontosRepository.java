package com.raizes.raizesback.infrastructure.repository;

import com.raizes.raizesback.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface MovimentoPontosRepository extends JpaRepository<MovimentoPontos, Long> {
    Page<MovimentoPontos> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);
}
