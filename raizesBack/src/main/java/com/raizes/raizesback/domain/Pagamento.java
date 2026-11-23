package com.raizes.raizesback.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "pagamentos")
@Getter @Setter @NoArgsConstructor
public class Pagamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pedido_id") private Pedido pedido;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusPagamento status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FormaPagamento formaPagamento;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal valor;
    private String transacaoId;
    private String mensagem;
    @Column(length = 2000) private String payloadGateway;
    @Column(nullable = false) private Instant criadoEm = Instant.now();

}
