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

@Service
@RequiredArgsConstructor
public class EstoqueService {
    private final EstoqueRepository estoques;
    private final MovimentacaoEstoqueRepository movimentacoes;
    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public PageResponse<EstoqueResponse> listar(Long unidadeId, int page, int limit) {
        if (!unidades.existsById(unidadeId)) {
            throw BusinessException.naoEncontrado("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.");
        }
        return PageResponse.of(estoques.findByUnidadeId(unidadeId, Paginacao.de(page, limit)).map(EstoqueResponse::from));
    }

    @Transactional
    public EstoqueResponse movimentar(MovimentacaoRequest r, AuthUser user) {
        Unidade u = unidades.findById(r.unidadeId()).orElseThrow(() ->
                BusinessException.naoEncontrado("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada."));
        Produto p = produtos.findById(r.produtoId()).filter(Produto::isAtivo).orElseThrow(() ->
                BusinessException.naoEncontrado("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado."));
        Estoque e = aplicar(u, p, r.tipo(), r.quantidade(), r.motivo(), user.id());
        auditoria.registrar(user.id(), "ESTOQUE_" + r.tipo(), "Estoque", e.getId(),
                "unidade=" + u.getId() + " produto=" + p.getId() + " qtd=" + r.quantidade());
        return EstoqueResponse.from(e);
    }

    /** Aplica entrada/saida com lock pessimista (evita venda acima do saldo em horarios de pico). */
    @Transactional
    public Estoque aplicar(Unidade u, Produto p, TipoMovimento tipo, int qtd, String motivo, Long usuarioId) {
        Estoque e = estoques.findForUpdate(u.getId(), p.getId()).orElseGet(() -> {
            Estoque novo = new Estoque();
            novo.setUnidade(u); novo.setProduto(p); novo.setQuantidade(0);
            return novo;
        });
        if (tipo == TipoMovimento.SAIDA) {
            if (e.getQuantidade() < qtd) {
                throw new BusinessException(HttpStatus.CONFLICT, "ESTOQUE_INSUFICIENTE",
                        "Não há quantidade suficiente em estoque.",
                        List.of(new FieldIssue("quantidade", "Disponível: " + e.getQuantidade())));
            }
            e.setQuantidade(e.getQuantidade() - qtd);
        } else {
            e.setQuantidade(e.getQuantidade() + qtd);
        }
        e = estoques.save(e);
        MovimentacaoEstoque m = new MovimentacaoEstoque();
        m.setUnidade(u); m.setProduto(p); m.setTipo(tipo); m.setQuantidade(qtd);
        m.setMotivo(motivo); m.setUsuarioId(usuarioId);
        movimentacoes.save(m);
        return e;
    }
}
