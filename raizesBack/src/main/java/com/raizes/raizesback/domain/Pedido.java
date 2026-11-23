package com.raizes.raizesback.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "pedidos")
@Getter @Setter @NoArgsConstructor
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cliente_id") private Usuario cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unidade_id") private Unidade unidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CanalPedido canalPedido;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusPedido status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FormaPagamento formaPagamento;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true) private List<ItemPedido> itens = new ArrayList<>();
    @Column(nullable = false) private Instant criadoEm = Instant.now();
    @Column(nullable = false) private Instant atualizadoEm = Instant.now();

}
