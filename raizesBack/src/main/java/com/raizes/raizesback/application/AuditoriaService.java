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
public class AuditoriaService {
    private final LogAuditoriaRepository repo;

    /** Registra acao sensivel (participa da transacao do chamador). */
    @Transactional
    public void registrar(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhes) {
        LogAuditoria l = new LogAuditoria();
        l.setUsuarioId(usuarioId); l.setAcao(acao); l.setEntidade(entidade);
        l.setEntidadeId(entidadeId); l.setDetalhes(detalhes);
        repo.save(l);
        log.info("AUDITORIA usuario={} acao={} entidade={}#{} {}", usuarioId, acao, entidade, entidadeId, detalhes);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditoriaResponse> listar(int page, int limit) {
        return PageResponse.of(repo.findAll(Paginacao.de(page, limit,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "criadoEm")))
                .map(AuditoriaResponse::from));
    }
}
