package com.raizes.raizesback.infrastructure.repository;

import com.raizes.raizesback.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByPedidoIdOrderByCriadoEmDesc(Long pedidoId);
}
