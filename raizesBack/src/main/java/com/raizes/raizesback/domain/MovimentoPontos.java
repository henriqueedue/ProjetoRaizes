package com.raizes.raizesback.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "movimentos_pontos")
@Getter @Setter @NoArgsConstructor
public class MovimentoPontos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @Column(nullable = false) private int pontos;
    @Column(nullable = false) private String tipo;
    private String descricao;
    @Column(nullable = false) private Instant criadoEm = Instant.now();

}
