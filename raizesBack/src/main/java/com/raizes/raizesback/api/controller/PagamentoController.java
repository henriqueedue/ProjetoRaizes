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
@RequestMapping("/pagamentos")
@Tag(name = "Pagamentos (mock)")
@RequiredArgsConstructor
public class PagamentoController {
    private final PagamentoService service;

    @Operation(summary = "Solicita pagamento ao gateway mock. simularResultado: APROVADO | RECUSADO | FALHA_GATEWAY")
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
    @PostMapping
    public ResponseEntity<PagamentoResponse> pagar(@Valid @RequestBody PagamentoRequest r,
                                                   @AuthenticationPrincipal AuthUser user) {
        PagamentoResponse resp = service.pagar(r, user);
        HttpStatus status = resp.status() == StatusPagamento.PENDENTE ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resp);
    }

    @Operation(summary = "Histórico de tentativas de pagamento de um pedido")
    @GetMapping("/pedido/{pedidoId}")
    public List<PagamentoResponse> porPedido(@PathVariable Long pedidoId, @AuthenticationPrincipal AuthUser user) {
        return service.listarPorPedido(pedidoId, user);
    }
}
