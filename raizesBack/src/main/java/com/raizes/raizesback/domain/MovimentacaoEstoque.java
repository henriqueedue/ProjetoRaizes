package com.raizes.raizesback.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "movimentacoes_estoque")
@Getter @Setter @NoArgsConstructor
public class MovimentacaoEstoque {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id") private Unidade unidade;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "produto_id") private Produto produto;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoMovimento tipo;
    @Column(nullable = false) private int quantidade;
    private String motivo;
    private Long usuarioId;
    @Column(nullable = false) private Instant criadoEm = Instant.now();

}
