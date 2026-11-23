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
@RequestMapping("/produtos")
@Tag(name = "Produtos")
@RequiredArgsConstructor
public class ProdutoController {
    private final ProdutoService service;

    @Operation(summary = "Lista produtos ativos (paginado)")
    @GetMapping
    public PageResponse<ProdutoResponse> listar(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int limit) {
        return service.listar(page, limit);
    }

    @Operation(summary = "Busca produto por id")
    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Cria produto (GERENTE/ADMIN)")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criar(@Valid @RequestBody ProdutoRequest r, @AuthenticationPrincipal AuthUser user) {
        return service.criar(r, user);
    }

    @Operation(summary = "Atualiza produto (GERENTE/ADMIN)")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest r,
                                     @AuthenticationPrincipal AuthUser user) {
        return service.atualizar(id, r, user);
    }

    @Operation(summary = "Desativa produto - exclusão lógica (GERENTE/ADMIN)")
    @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        service.desativar(id, user);
    }
}
