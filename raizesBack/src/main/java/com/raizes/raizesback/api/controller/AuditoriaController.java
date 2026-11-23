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
@RequestMapping("/auditoria")
@Tag(name = "Auditoria")
@RequiredArgsConstructor
public class AuditoriaController {
    private final AuditoriaService service;

    @Operation(summary = "Lista logs de auditoria (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<AuditoriaResponse> listar(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int limit) {
        return service.listar(page, limit);
    }
}
