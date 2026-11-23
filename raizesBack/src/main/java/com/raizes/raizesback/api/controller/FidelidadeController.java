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
@RequestMapping("/fidelidade")
@Tag(name = "Fidelidade")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class FidelidadeController {
    private final FidelidadeService service;

    @Operation(summary = "Saldo de pontos e status do consentimento")
    @GetMapping("/saldo")
    public SaldoResponse saldo(@AuthenticationPrincipal AuthUser user) {
        return service.saldo(user);
    }

    @Operation(summary = "Concede ou revoga consentimento (LGPD) para o programa de fidelidade")
    @PutMapping("/consentimento")
    public SaldoResponse consentimento(@Valid @RequestBody ConsentimentoRequest r, @AuthenticationPrincipal AuthUser user) {
        return service.consentimento(user, r.aceito());
    }

    @Operation(summary = "Resgata pontos (1 ponto = R$ 0,05 de desconto)")
    @PostMapping("/resgatar")
    public ResgateResponse resgatar(@Valid @RequestBody ResgateRequest r, @AuthenticationPrincipal AuthUser user) {
        return service.resgatar(user, r.pontos());
    }

    @Operation(summary = "Histórico de movimentos de pontos")
    @GetMapping("/historico")
    public PageResponse<MovimentoPontosResponse> historico(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int limit,
                                                           @AuthenticationPrincipal AuthUser user) {
        return service.historico(user, page, limit);
    }
}
