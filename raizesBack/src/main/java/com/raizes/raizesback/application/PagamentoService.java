package com.raizes.raizesback.application;

import com.raizes.raizesback.api.dto.Dtos.*;
import com.raizes.raizesback.api.exception.BusinessException;
import com.raizes.raizesback.domain.*;
import com.raizes.raizesback.infrastructure.repository.*;
import com.raizes.raizesback.infrastructure.security.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {
    private final PagamentoRepository pagamentos;
    private final PedidoService pedidoService;
    private final com.raizes.raizesback.infrastructure.payment.MockPaymentGateway gateway;
    private final AuditoriaService auditoria;

    /**
     * Solicita pagamento ao gateway (mock) e atualiza o pedido:
     * APROVADO -> pedido PAGO | RECUSADO -> pedido CANCELADO + estoque devolvido |
     * gateway indisponivel -> pagamento PENDENTE e pedido segue AGUARDANDO_PAGAMENTO (pode tentar de novo).
     */
    @Transactional
    public PagamentoResponse pagar(PagamentoRequest r, AuthUser user) {
        Pedido pedido = pedidoService.carregarComAcesso(r.pedidoId(), user);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw BusinessException.conflito("PEDIDO_NAO_AGUARDA_PAGAMENTO",
                    "Pedido no status " + pedido.getStatus() + " não aceita novo pagamento.");
        }

        Pagamento pg = new Pagamento();
        pg.setPedido(pedido);
        pg.setValor(pedido.getTotal());
        pg.setFormaPagamento(pedido.getFormaPagamento());
        try {
            var resp = gateway.processar(pedido.getId(), pedido.getTotal(), pedido.getFormaPagamento(), r.simularResultado());
            pg.setStatus(resp.status());
            pg.setTransacaoId(resp.transacaoId());
            pg.setMensagem(resp.mensagem());
            pg.setPayloadGateway(resp.payload());
        } catch (com.raizes.raizesback.infrastructure.payment.GatewayIndisponivelException e) {
            log.warn("Gateway indisponivel para pedido {}: {}", pedido.getId(), e.getMessage());
            pg.setStatus(StatusPagamento.PENDENTE);
            pg.setMensagem("Gateway de pagamento indisponível. Tente novamente; o pedido foi mantido.");
        }

        if (pg.getStatus() == StatusPagamento.APROVADO) {
            pedido.setStatus(StatusPedido.PAGO);
            pedido.setAtualizadoEm(Instant.now());
        } else if (pg.getStatus() == StatusPagamento.RECUSADO) {
            pedidoService.cancelarInterno(pedido, "Pagamento recusado do pedido #" + pedido.getId(), user.id());
            pg.setMensagem(pg.getMensagem() + " Pedido cancelado e estoque devolvido.");
        }
        pg = pagamentos.save(pg);
        auditoria.registrar(user.id(), "PAGAMENTO_" + pg.getStatus(), "Pedido", pedido.getId(),
                "transacao=" + pg.getTransacaoId() + " valor=" + pg.getValor());
        return PagamentoResponse.from(pg);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listarPorPedido(Long pedidoId, AuthUser user) {
        pedidoService.carregarComAcesso(pedidoId, user);
        return pagamentos.findByPedidoIdOrderByCriadoEmDesc(pedidoId).stream().map(PagamentoResponse::from).toList();
    }
}
