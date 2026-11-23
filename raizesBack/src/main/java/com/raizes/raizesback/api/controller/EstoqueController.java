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
@RequestMapping("/estoque")
@Tag(name = "Estoque")
@RequiredArgsConstructor
public class EstoqueController {
    private final EstoqueService service;

    @Operation(summary = "Saldo de estoque por unidade")
    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE','ADMIN')")
    @GetMapping
    public PageResponse<EstoqueResponse> listar(@RequestParam Long unidadeId, @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int limit) {
        return service.listar(unidadeId, page, limit);
    }

    @Operation(summary = "Movimenta estoque (ENTRADA/SAIDA) - GERENTE/ADMIN")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    @PostMapping("/movimentacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public EstoqueResponse movimentar(@Valid @RequestBody MovimentacaoRequest r, @AuthenticationPrincipal AuthUser user) {
        return service.movimentar(r, user);
    }
}
