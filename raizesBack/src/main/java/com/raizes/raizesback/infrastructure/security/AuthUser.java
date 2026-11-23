package com.raizes.raizesback.infrastructure.security;

import com.raizes.raizesback.domain.Perfil;

/** Usuario autenticado extraido do JWT. */
public record AuthUser(Long id, String email, Perfil perfil) {}
