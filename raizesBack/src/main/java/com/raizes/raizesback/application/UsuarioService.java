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
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public UsuarioResponse me(AuthUser user) {
        return UsuarioResponse.from(carregar(user.id()));
    }

    @Transactional
    public UsuarioResponse criarFuncionario(FuncionarioRequest r, AuthUser admin) {
        String email = r.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw BusinessException.conflito("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(r.nome().trim()); u.setEmail(email);
        u.setSenhaHash(encoder.encode(r.senha())); u.setPerfil(r.perfil());
        u = usuarios.save(u);
        auditoria.registrar(admin.id(), "USUARIO_CRIADO_POR_ADMIN", "Usuario", u.getId(), "perfil=" + r.perfil());
        return UsuarioResponse.from(u);
    }

    /** LGPD: direito a eliminacao/anonimizacao. Pedidos sao mantidos sem dados pessoais (retencao fiscal). */
    @Transactional
    public void anonimizar(AuthUser user) {
        Usuario u = carregar(user.id());
        u.setNome("Usuário anonimizado");
        u.setEmail("anonimizado-" + u.getId() + "@raizes.invalid");
        u.setSenhaHash(encoder.encode(UUID.randomUUID().toString()));
        u.setAtivo(false);
        u.setConsentimentoLgpd(false);
        u.setFidelidadeConsentimento(false);
        u.setPontosFidelidade(0);
        usuarios.save(u);
        auditoria.registrar(u.getId(), "USUARIO_ANONIMIZADO", "Usuario", u.getId(), "solicitação do titular (LGPD)");
    }

    Usuario carregar(Long id) {
        return usuarios.findById(id).orElseThrow(() ->
                BusinessException.naoEncontrado("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }
}
