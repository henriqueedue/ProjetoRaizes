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
public class AuthService {
    private final UsuarioRepository usuarios;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditoriaService auditoria;

    /** Cadastro publico: sempre cria perfil CLIENTE e registra consentimento LGPD. */
    @Transactional
    public UsuarioResponse registrar(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw BusinessException.conflito("EMAIL_JA_CADASTRADO", "Já existe um usuário com este e-mail.");
        }
        Usuario u = new Usuario();
        u.setNome(r.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(encoder.encode(r.senha()));
        u.setPerfil(Perfil.CLIENTE);
        u.setConsentimentoLgpd(true);
        u.setConsentimentoLgpdEm(Instant.now());
        u = usuarios.save(u);
        auditoria.registrar(u.getId(), "USUARIO_CADASTRADO", "Usuario", u.getId(), "consentimentoLgpd=true");
        return UsuarioResponse.from(u);
    }

    @Transactional
    public LoginResponse login(LoginRequest r) {
        Usuario u = usuarios.findByEmail(r.email().trim().toLowerCase())
                .filter(Usuario::isAtivo)
                .filter(x -> encoder.matches(r.senha(), x.getSenhaHash()))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS",
                        "E-mail ou senha inválidos."));
        auditoria.registrar(u.getId(), "LOGIN", "Usuario", u.getId(), null);
        return new LoginResponse(jwt.gerar(u), "Bearer", jwt.getExpirationSeconds(),
                new UserResumo(u.getId(), u.getNome(), u.getPerfil()));
    }
}
