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
public class FidelidadeService {
    /** Regra: 1 ponto a cada R$ 1,00 pago. 1 ponto resgatado = R$ 0,05 de desconto. */
    private static final BigDecimal VALOR_PONTO = new BigDecimal("0.05");

    private final UsuarioRepository usuarios;
    private final MovimentoPontosRepository movimentos;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public SaldoResponse saldo(AuthUser user) {
        Usuario u = carregar(user.id());
        return new SaldoResponse(u.getPontosFidelidade(), u.isFidelidadeConsentimento());
    }

    @Transactional
    public SaldoResponse consentimento(AuthUser user, boolean aceito) {
        Usuario u = carregar(user.id());
        u.setFidelidadeConsentimento(aceito);
        usuarios.save(u);
        auditoria.registrar(u.getId(), "FIDELIDADE_CONSENTIMENTO", "Usuario", u.getId(), "aceito=" + aceito);
        return new SaldoResponse(u.getPontosFidelidade(), aceito);
    }

    /** Acumulo automatico na entrega do pedido (somente com consentimento). */
    @Transactional
    public void creditar(Pedido pedido) {
        Usuario c = pedido.getCliente();
        if (c == null || !c.isFidelidadeConsentimento()) return;
        int pontos = pedido.getTotal().intValue();
        if (pontos <= 0) return;
        c.setPontosFidelidade(c.getPontosFidelidade() + pontos);
        usuarios.save(c);
        registrarMovimento(c, pontos, "ACUMULO", "Pedido #" + pedido.getId());
        auditoria.registrar(c.getId(), "PONTOS_ACUMULADOS", "Pedido", pedido.getId(), "+" + pontos);
    }

    @Transactional
    public ResgateResponse resgatar(AuthUser user, int pontos) {
        Usuario u = carregar(user.id());
        if (!u.isFidelidadeConsentimento()) {
            throw BusinessException.conflito("CONSENTIMENTO_NECESSARIO",
                    "É necessário consentir com o programa de fidelidade antes de resgatar pontos.");
        }
        if (u.getPontosFidelidade() < pontos) {
            throw new BusinessException(HttpStatus.CONFLICT, "SALDO_INSUFICIENTE", "Saldo de pontos insuficiente.",
                    List.of(new FieldIssue("pontos", "Saldo: " + u.getPontosFidelidade())));
        }
        u.setPontosFidelidade(u.getPontosFidelidade() - pontos);
        usuarios.save(u);
        registrarMovimento(u, -pontos, "RESGATE", "Resgate de pontos");
        auditoria.registrar(u.getId(), "PONTOS_RESGATADOS", "Usuario", u.getId(), "-" + pontos);
        return new ResgateResponse(pontos, VALOR_PONTO.multiply(BigDecimal.valueOf(pontos)), u.getPontosFidelidade());
    }

    @Transactional(readOnly = true)
    public PageResponse<MovimentoPontosResponse> historico(AuthUser user, int page, int limit) {
        return PageResponse.of(movimentos.findByUsuarioIdOrderByCriadoEmDesc(user.id(), Paginacao.de(page, limit))
                .map(MovimentoPontosResponse::from));
    }

    private void registrarMovimento(Usuario u, int pontos, String tipo, String descricao) {
        MovimentoPontos m = new MovimentoPontos();
        m.setUsuario(u); m.setPontos(pontos); m.setTipo(tipo); m.setDescricao(descricao);
        movimentos.save(m);
    }

    private Usuario carregar(Long id) {
        return usuarios.findById(id).orElseThrow(() ->
                BusinessException.naoEncontrado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }
}
