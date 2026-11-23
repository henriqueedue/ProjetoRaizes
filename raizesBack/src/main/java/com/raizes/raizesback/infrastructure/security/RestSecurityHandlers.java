package com.raizes.raizesback.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizes.raizesback.api.dto.Dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Respostas JSON padronizadas para 401 e 403 geradas pela camada de seguranca. */
@Component
@RequiredArgsConstructor
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        write(req, res, 401, "NAO_AUTENTICADO", "Autenticação necessária: informe um token Bearer válido.");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        write(req, res, 403, "ACESSO_NEGADO", "Seu perfil não tem permissão para esta operação.");
    }

    private void write(HttpServletRequest req, HttpServletResponse res, int status, String error, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), ErrorResponse.of(error, msg, null, req.getRequestURI()));
    }
}
