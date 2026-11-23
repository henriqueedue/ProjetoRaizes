package com.raizes.raizesback.application;

import com.raizes.raizesback.api.dto.Dtos.*;
import com.raizes.raizesback.api.exception.BusinessException;
import com.raizes.raizesback.domain.*;
import com.raizes.raizesback.infrastructure.repository.*;
import com.raizes.raizesback.infrastructure.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
public class PedidoService {
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final EstoqueRepository estoques;
    private final EstoqueService estoqueService;
    private final FidelidadeService fidelidade;
    private final AuditoriaService auditoria;

    /** Fluxo critico: valida unidade/produtos, calcula total, reserva estoque e cria pedido AGUARDANDO_PAGAMENTO. */
    @Transactional
    public PedidoResponse criar(PedidoRequest r, AuthUser user) {
        Usuario cliente = resolverCliente(r.clienteId(), user);
        Unidade unidade = unidades.findById(r.unidadeId()).orElseThrow(() ->
                BusinessException.naoEncontrado("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada."));
        if (!unidade.isAtiva()) {
            throw BusinessException.conflito("UNIDADE_INATIVA", "A unidade informada não está ativa.");
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUnidade(unidade);
        pedido.setCanalPedido(r.canalPedido());
        pedido.setFormaPagamento(r.formaPagamento());
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest ir : r.itens()) {
            Produto p = produtos.findById(ir.produtoId()).filter(Produto::isAtivo).orElseThrow(() ->
                    BusinessException.naoEncontrado("PRODUTO_NAO_ENCONTRADO", "Produto " + ir.produtoId() + " não encontrado."));
            ItemPedido item = new ItemPedido();
            item.setPedido(pedido); item.setProduto(p);
            item.setQuantidade(ir.quantidade()); item.setPrecoUnitario(p.getPreco());
            pedido.getItens().add(item);
            total = total.add(p.getPreco().multiply(BigDecimal.valueOf(ir.quantidade())));
        }
        pedido.setTotal(total);
        pedido = pedidos.save(pedido);

        for (int i = 0; i < r.itens().size(); i++) {
            ItemPedido item = pedido.getItens().get(i);
            int disponivel = estoques.findForUpdate(unidade.getId(), item.getProduto().getId())
                    .map(Estoque::getQuantidade).orElse(0);
            if (disponivel < item.getQuantidade()) {
                throw new BusinessException(HttpStatus.CONFLICT, "ESTOQUE_INSUFICIENTE",
                        "Não há quantidade suficiente para um ou mais itens.",
                        List.of(new FieldIssue("itens[" + i + "].quantidade", "Disponível: " + disponivel)));
            }
            estoqueService.aplicar(unidade, item.getProduto(), TipoMovimento.SAIDA, item.getQuantidade(),
                    "Reserva do pedido #" + pedido.getId(), user.id());
        }
        auditoria.registrar(user.id(), "PEDIDO_CRIADO", "Pedido", pedido.getId(),
                "canal=" + pedido.getCanalPedido() + " total=" + total);
        return PedidoResponse.from(pedido);
    }

    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> listar(CanalPedido canal, StatusPedido status, Long unidadeId,
                                               int page, int limit, AuthUser user) {
        Long clienteId = user.perfil() == Perfil.CLIENTE ? user.id() : null; // cliente so enxerga os proprios pedidos
        Specification<Pedido> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (canal != null) ps.add(cb.equal(root.get("canalPedido"), canal));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (unidadeId != null) ps.add(cb.equal(root.get("unidade").get("id"), unidadeId));
            if (clienteId != null) ps.add(cb.equal(root.get("cliente").get("id"), clienteId));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(pedidos.findAll(spec,
                Paginacao.de(page, limit, Sort.by(Sort.Direction.DESC, "criadoEm"))).map(PedidoResponse::from));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long id, AuthUser user) {
        return PedidoResponse.from(carregarComAcesso(id, user));
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long id, StatusPedido novo, AuthUser user) {
        Pedido p = carregar(id);
        if (novo == StatusPedido.CANCELADO) return cancelar(id, user);
        StatusPedido atual = p.getStatus();
        if (!atual.podeTransicionarPara(novo)) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRANSICAO_INVALIDA",
                    "Transição de status não permitida: " + atual + " -> " + novo + ".",
                    List.of(new FieldIssue("status", "Fluxo: PAGO -> EM_PREPARO -> PRONTO -> ENTREGUE")));
        }
        p.setStatus(novo);
        p.setAtualizadoEm(Instant.now());
        if (novo == StatusPedido.ENTREGUE) fidelidade.creditar(p);
        auditoria.registrar(user.id(), "PEDIDO_STATUS_ALTERADO", "Pedido", p.getId(), atual + " -> " + novo);
        return PedidoResponse.from(pedidos.save(p));
    }

    @Transactional
    public PedidoResponse cancelar(Long id, AuthUser user) {
        Pedido p = carregarComAcesso(id, user);
        boolean cliente = user.perfil() == Perfil.CLIENTE;
        boolean permitido = p.getStatus().cancelavel()
                && !(cliente && p.getStatus() == StatusPedido.EM_PREPARO);
        if (!permitido) {
            throw BusinessException.conflito("PEDIDO_NAO_CANCELAVEL",
                    "Pedido no status " + p.getStatus() + " não pode ser cancelado por este perfil.");
        }
        StatusPedido anterior = p.getStatus();
        cancelarInterno(p, "Cancelamento do pedido #" + p.getId(), user.id());
        auditoria.registrar(user.id(), "PEDIDO_CANCELADO", "Pedido", p.getId(), "status anterior=" + anterior);
        return PedidoResponse.from(p);
    }

    /** Cancela o pedido e devolve o estoque reservado (usado tambem quando o pagamento e recusado). */
    @Transactional
    public void cancelarInterno(Pedido p, String motivo, Long usuarioId) {
        for (ItemPedido it : p.getItens()) {
            estoqueService.aplicar(p.getUnidade(), it.getProduto(), TipoMovimento.ENTRADA, it.getQuantidade(), motivo, usuarioId);
        }
        p.setStatus(StatusPedido.CANCELADO);
        p.setAtualizadoEm(Instant.now());
        pedidos.save(p);
    }

    public Pedido carregar(Long id) {
        return pedidos.findById(id).orElseThrow(() ->
                BusinessException.naoEncontrado("PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));
    }

    public Pedido carregarComAcesso(Long id, AuthUser user) {
        Pedido p = carregar(id);
        if (user.perfil() == Perfil.CLIENTE
                && (p.getCliente() == null || !p.getCliente().getId().equals(user.id()))) {
            throw BusinessException.proibido("Este pedido pertence a outro cliente.");
        }
        return p;
    }

    private Usuario resolverCliente(Long clienteId, AuthUser user) {
        if (user.perfil() == Perfil.CLIENTE) {
            if (clienteId != null && !clienteId.equals(user.id())) {
                throw BusinessException.proibido("Um cliente só pode criar pedidos para si mesmo.");
            }
            return usuarios.findById(user.id()).orElseThrow(() ->
                    BusinessException.naoEncontrado("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
        }
        if (clienteId == null) return null; // venda de balcao sem cadastro
        return usuarios.findById(clienteId).orElseThrow(() ->
                BusinessException.naoEncontrado("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
    }
}
