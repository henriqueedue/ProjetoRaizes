package com.raizes.raizesback.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String nome;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Perfil perfil;
    private boolean ativo = true;
    private boolean consentimentoLgpd;
    private Instant consentimentoLgpdEm;
    private boolean fidelidadeConsentimento;
    private int pontosFidelidade;
    @Column(nullable = false) private Instant criadoEm = Instant.now();

}
