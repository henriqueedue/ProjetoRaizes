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
@RequestMapping("/pedidos")
@Tag(name = "Pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService service;

    @Operation(summary = "Cria pedido (exige canalPedido); valida estoque e reserva itens")
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody PedidoRequest r, @AuthenticationPrincipal AuthUser user) {
        return service.criar(r, user);
    }

    @Operation(summary = "Lista pedidos com filtros ?canalPedido=&status=&unidadeId=&page=&limit=")
    @GetMapping
    public PageResponse<PedidoResponse> listar(@RequestParam(required = false) CanalPedido canalPedido,
                                               @RequestParam(required = false) StatusPedido status,
                                               @RequestParam(required = false) Long unidadeId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int limit,
                                               @AuthenticationPrincipal AuthUser user) {
        return service.listar(canalPedido, status, unidadeId, page, limit, user);
    }

    @Operation(summary = "Busca pedido por id")
    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.buscar(id, user);
    }

    @Operation(summary = "Atualiza status: PAGO -> EM_PREPARO -> PRONTO -> ENTREGUE (equipe)")
    @PreAuthorize("hasAnyRole('COZINHA','ATENDENTE','GERENTE','ADMIN')")
    @PatchMapping("/{id}/status")
    public PedidoResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r,
                                 @AuthenticationPrincipal AuthUser user) {
        return service.atualizarStatus(id, r.status(), user);
    }

    @Operation(summary = "Cancela pedido e devolve estoque")
    @PatchMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.cancelar(id, user);
    }
}
