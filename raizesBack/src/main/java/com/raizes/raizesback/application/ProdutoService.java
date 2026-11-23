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
public class ProdutoService {
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public PageResponse<ProdutoResponse> listar(int page, int limit) {
        return PageResponse.of(produtos.findByAtivoTrue(Paginacao.de(page, limit)).map(ProdutoResponse::from));
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return ProdutoResponse.from(carregar(id));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest r, AuthUser user) {
        Produto p = new Produto();
        preencher(p, r);
        p = produtos.save(p);
        auditoria.registrar(user.id(), "PRODUTO_CRIADO", "Produto", p.getId(), p.getNome());
        return ProdutoResponse.from(p);
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest r, AuthUser user) {
        Produto p = carregar(id);
        preencher(p, r);
        auditoria.registrar(user.id(), "PRODUTO_ATUALIZADO", "Produto", p.getId(), "preco=" + p.getPreco());
        return ProdutoResponse.from(produtos.save(p));
    }

    /** Exclusao logica: preserva historico de pedidos. */
    @Transactional
    public void desativar(Long id, AuthUser user) {
        Produto p = carregar(id);
        p.setAtivo(false);
        produtos.save(p);
        auditoria.registrar(user.id(), "PRODUTO_DESATIVADO", "Produto", p.getId(), p.getNome());
    }

    private void preencher(Produto p, ProdutoRequest r) {
        p.setNome(r.nome().trim()); p.setDescricao(r.descricao()); p.setPreco(r.preco());
    }

    private Produto carregar(Long id) {
        return produtos.findById(id).filter(Produto::isAtivo).orElseThrow(() ->
                BusinessException.naoEncontrado("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado."));
    }
}
