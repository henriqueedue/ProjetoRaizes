package com.raizes.raizesback.domain;

/** Maquina de estados do pedido (regra de dominio). */
public enum StatusPedido {
    AGUARDANDO_PAGAMENTO, PAGO, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO;

    /** Transicoes manuais permitidas (cozinha/atendente). PAGO so e atingido via pagamento. */
    public boolean podeTransicionarPara(StatusPedido novo) {
        return switch (this) {
            case PAGO -> novo == EM_PREPARO;
            case EM_PREPARO -> novo == PRONTO;
            case PRONTO -> novo == ENTREGUE;
            default -> false;
        };
    }

    public boolean cancelavel() {
        return this == AGUARDANDO_PAGAMENTO || this == PAGO || this == EM_PREPARO;
    }
}
