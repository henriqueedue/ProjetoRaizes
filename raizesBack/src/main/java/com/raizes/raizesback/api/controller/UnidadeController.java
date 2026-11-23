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
@RequestMapping("/unidades")
@Tag(name = "Unidades")
@RequiredArgsConstructor
public class UnidadeController {
    private final UnidadeService service;

    @Operation(summary = "Lista unidades ativas (público)")
    @GetMapping
    public PageResponse<UnidadeResponse> listar(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int limit) {
        return service.listar(page, limit);
    }

    @Operation(summary = "Cardápio da unidade com disponibilidade (público)")
    @GetMapping("/{id}/cardapio")
    public PageResponse<CardapioItem> cardapio(@PathVariable Long id, @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int limit) {
        return service.cardapio(id, page, limit);
    }

    @Operation(summary = "Cria unidade (GERENTE/ADMIN)")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UnidadeResponse criar(@Valid @RequestBody UnidadeRequest r, @AuthenticationPrincipal AuthUser user) {
        return service.criar(r, user);
    }
}
