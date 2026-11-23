package com.raizes.raizesback.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "logs_auditoria")
@Getter @Setter @NoArgsConstructor
public class LogAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    @Column(nullable = false) private String acao;
    private String entidade;
    private Long entidadeId;
    @Column(length = 1000) private String detalhes;
    @Column(nullable = false) private Instant criadoEm = Instant.now();

}
