package com.raizes.raizesback.api.dto;

import com.raizes.raizesback.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contratos (request/response) da API agrupados por recurso. */
public final class Dtos {
    private Dtos() {}

    // ===== Padrao de paginacao e de erro =====
    public record PageResponse<T>(List<T> content, int page, int limit, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber() + 1, p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record FieldIssue(String field, String issue) {}

    public record ErrorResponse(String error, String message, List<FieldIssue> details, Instant timestamp, String path) {
        public static ErrorResponse of(String error, String message, List<FieldIssue> details, String path) {
            return new ErrorResponse(error, message, details == null ? List.of() : details, Instant.now(), path);
        }
    }

    // ===== Auth / Usuarios =====
    public record RegisterRequest(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @AssertTrue(message = "É necessário aceitar os termos de privacidade (LGPD).") boolean aceiteLgpd) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}

    public record FuncionarioRequest(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @NotNull Perfil perfil) {}

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil,
                                  boolean fidelidadeConsentimento, int pontosFidelidade) {
        public static UsuarioResponse from(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                    u.isFidelidadeConsentimento(), u.getPontosFidelidade());
        }
    }

    public record UserResumo(Long id, String nome, Perfil perfil) {}

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResumo user) {}

    // ===== Unidades / Produtos / Estoque =====
    public record UnidadeRequest(@NotBlank String nome, @NotBlank String cidade) {}

    public record UnidadeResponse(Long id, String nome, String cidade, boolean ativa) {
        public static UnidadeResponse from(Unidade u) {
            return new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.isAtiva());
        }
    }

    public record ProdutoRequest(@NotBlank String nome, @Size(max = 300) String descricao,
                                 @NotNull @DecimalMin("0.01") BigDecimal preco) {}

    public record ProdutoResponse(Long id, String nome, String descricao, BigDecimal preco, boolean ativo) {
        public static ProdutoResponse from(Produto p) {
            return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(), p.isAtivo());
        }
    }

    public record CardapioItem(Long produtoId, String nome, String descricao, BigDecimal preco,
                               int quantidadeDisponivel, boolean disponivel) {
        public static CardapioItem from(Estoque e) {
            Produto p = e.getProduto();
            return new CardapioItem(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(),
                    e.getQuantidade(), e.getQuantidade() > 0);
        }
    }

    public record MovimentacaoRequest(@NotNull Long unidadeId, @NotNull Long produtoId,
                                      @NotNull TipoMovimento tipo, @NotNull @Min(1) Integer quantidade,
                                      @Size(max = 200) String motivo) {}

    public record EstoqueResponse(Long unidadeId, Long produtoId, String produtoNome, int quantidade) {
        public static EstoqueResponse from(Estoque e) {
            return new EstoqueResponse(e.getUnidade().getId(), e.getProduto().getId(),
                    e.getProduto().getNome(), e.getQuantidade());
        }
    }

    // ===== Pedidos =====
    public record ItemRequest(@NotNull Long produtoId, @NotNull @Min(1) Integer quantidade) {}

    public record PedidoRequest(
            @NotNull CanalPedido canalPedido,
            Long clienteId,
            @NotNull Long unidadeId,
            @NotEmpty @Valid List<ItemRequest> itens,
            @NotNull FormaPagamento formaPagamento) {}

    public record StatusRequest(@NotNull StatusPedido status) {}

    public record ItemResponse(Long produtoId, String nome, int quantidade, BigDecimal precoUnitario) {}

    public record PedidoResponse(Long pedidoId, CanalPedido canalPedido, StatusPedido status, Long unidadeId,
                                 Long clienteId, BigDecimal total, FormaPagamento formaPagamento,
                                 List<ItemResponse> itens, Instant createdAt) {
        public static PedidoResponse from(Pedido p) {
            return new PedidoResponse(p.getId(), p.getCanalPedido(), p.getStatus(), p.getUnidade().getId(),
                    p.getCliente() == null ? null : p.getCliente().getId(), p.getTotal(), p.getFormaPagamento(),
                    p.getItens().stream().map(i -> new ItemResponse(i.getProduto().getId(), i.getProduto().getNome(),
                            i.getQuantidade(), i.getPrecoUnitario())).toList(),
                    p.getCriadoEm());
        }
    }

    // ===== Pagamentos (mock) =====
    public record PagamentoRequest(@NotNull Long pedidoId, ResultadoSimulado simularResultado) {}

    public record PagamentoResponse(Long pagamentoId, Long pedidoId, StatusPagamento status, BigDecimal valor,
                                    String transacaoId, String mensagem, String payloadGateway,
                                    StatusPedido pedidoStatus, Instant criadoEm) {
        public static PagamentoResponse from(Pagamento g) {
            return new PagamentoResponse(g.getId(), g.getPedido().getId(), g.getStatus(), g.getValor(),
                    g.getTransacaoId(), g.getMensagem(), g.getPayloadGateway(),
                    g.getPedido().getStatus(), g.getCriadoEm());
        }
    }

    // ===== Fidelidade =====
    public record ConsentimentoRequest(@NotNull Boolean aceito) {}

    public record ResgateRequest(@NotNull @Min(1) Integer pontos) {}

    public record SaldoResponse(int pontos, boolean consentimento) {}

    public record ResgateResponse(int pontosResgatados, BigDecimal valorDesconto, int saldoRestante) {}

    public record MovimentoPontosResponse(Long id, int pontos, String tipo, String descricao, Instant criadoEm) {
        public static MovimentoPontosResponse from(MovimentoPontos m) {
            return new MovimentoPontosResponse(m.getId(), m.getPontos(), m.getTipo(), m.getDescricao(), m.getCriadoEm());
        }
    }

    // ===== Auditoria =====
    public record AuditoriaResponse(Long id, Long usuarioId, String acao, String entidade, Long entidadeId,
                                    String detalhes, Instant criadoEm) {
        public static AuditoriaResponse from(LogAuditoria l) {
            return new AuditoriaResponse(l.getId(), l.getUsuarioId(), l.getAcao(), l.getEntidade(),
                    l.getEntidadeId(), l.getDetalhes(), l.getCriadoEm());
        }
    }
}
