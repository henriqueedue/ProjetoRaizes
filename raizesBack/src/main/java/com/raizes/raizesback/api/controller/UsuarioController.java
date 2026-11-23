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
@RequestMapping("/usuarios")
@Tag(name = "Usuários")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService service;

    @Operation(summary = "Perfil do usuário autenticado")
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal AuthUser user) {
        return service.me(user);
    }

    @Operation(summary = "ADMIN cria usuário de equipe (ATENDENTE, COZINHA, GERENTE...)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody FuncionarioRequest r, @AuthenticationPrincipal AuthUser user) {
        return service.criarFuncionario(r, user);
    }

    @Operation(summary = "LGPD: anonimiza os dados pessoais do próprio usuário")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonimizar(@AuthenticationPrincipal AuthUser user) {
        service.anonimizar(user);
    }
}
