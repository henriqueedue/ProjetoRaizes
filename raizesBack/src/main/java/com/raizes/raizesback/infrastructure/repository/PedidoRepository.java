package com.raizes.raizesback.infrastructure.repository;

import com.raizes.raizesback.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Pedido> {

}
