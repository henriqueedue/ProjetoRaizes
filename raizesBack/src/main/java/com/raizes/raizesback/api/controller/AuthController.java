package com.raizes.raizesback.api.controller;

import com.raizes.raizesback.api.dto.Dtos.*;
import com.raizes.raizesback.application.*;
import com.raizes.raizesback.domain.*;
import com.raizes.raizesback.infrastructure.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @Operation(summary = "Cadastro de cliente (exige aceite LGPD)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse register(@Valid @RequestBody RegisterRequest r) {
        return service.registrar(r);
    }

    @Operation(summary = "Login: retorna token JWT")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        return service.login(r);
    }
}
