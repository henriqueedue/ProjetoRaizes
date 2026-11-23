package com.raizes.raizesback.infrastructure.repository;

import com.raizes.raizesback.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    Page<Estoque> findByUnidadeId(Long unidadeId, Pageable pageable);
    Page<Estoque> findByUnidadeIdAndProdutoAtivoTrue(Long unidadeId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.unidade.id = :unidadeId and e.produto.id = :produtoId")
    Optional<Estoque> findForUpdate(@Param("unidadeId") Long unidadeId, @Param("produtoId") Long produtoId);
}
