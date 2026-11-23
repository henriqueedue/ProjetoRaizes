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
public class UnidadeService {
    private final UnidadeRepository unidades;
    private final EstoqueRepository estoques;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public PageResponse<UnidadeResponse> listar(int page, int limit) {
        return PageResponse.of(unidades.findByAtivaTrue(Paginacao.de(page, limit)).map(UnidadeResponse::from));
    }

    @Transactional
    public UnidadeResponse criar(UnidadeRequest r, AuthUser user) {
        Unidade u = new Unidade();
        u.setNome(r.nome().trim()); u.setCidade(r.cidade().trim());
        u = unidades.save(u);
        auditoria.registrar(user.id(), "UNIDADE_CRIADA", "Unidade", u.getId(), u.getNome());
        return UnidadeResponse.from(u);
    }

    /** Cardapio da unidade = produtos ativos com registro de estoque nessa unidade (+ disponibilidade). */
    @Transactional(readOnly = true)
    public PageResponse<CardapioItem> cardapio(Long unidadeId, int page, int limit) {
        if (!unidades.existsById(unidadeId)) {
            throw BusinessException.naoEncontrado("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.");
        }
        return PageResponse.of(estoques.findByUnidadeIdAndProdutoAtivoTrue(unidadeId, Paginacao.de(page, limit))
                .map(CardapioItem::from));
    }
}
