package com.raizes.raizesback.infrastructure.payment;

import com.raizes.raizesback.domain.FormaPagamento;
import com.raizes.raizesback.domain.ResultadoSimulado;
import com.raizes.raizesback.domain.StatusPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/** Simula um gateway externo de pagamento. Nenhuma cobranca real e feita. */
@Component
public class MockPaymentGateway {

    public record GatewayResponse(StatusPagamento status, String transacaoId, String mensagem, String payload) {}

    /**
     * @param simulado null/APROVADO -> aprova; RECUSADO -> recusa; FALHA_GATEWAY -> lanca indisponibilidade
     */
    public GatewayResponse processar(Long pedidoId, BigDecimal valor, FormaPagamento forma, ResultadoSimulado simulado) {
        if (simulado == ResultadoSimulado.FALHA_GATEWAY) {
            throw new GatewayIndisponivelException("Timeout ao contatar o gateway de pagamento (mock).");
        }
        boolean aprovado = simulado == null || simulado == ResultadoSimulado.APROVADO;
        StatusPagamento status = aprovado ? StatusPagamento.APROVADO : StatusPagamento.RECUSADO;
        String tx = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String msg = aprovado ? "Pagamento aprovado (mock)." : "Pagamento recusado pela operadora (mock).";
        String payload = String.format(Locale.ROOT,
                "{\"transactionId\":\"%s\",\"status\":\"%s\",\"amount\":%s,\"method\":\"%s\",\"orderId\":%d}",
                tx, status, valor.toPlainString(), forma, pedidoId);
        return new GatewayResponse(status, tx, msg, payload);
    }
}
