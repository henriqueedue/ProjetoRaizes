package com.raizes.raizesback.api.exception;

import com.raizes.raizesback.api.dto.Dtos.ErrorResponse;
import com.raizes.raizesback.api.dto.Dtos.FieldIssue;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Padrao unico de erro: { error, message, details[], timestamp, path }. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String error, String msg, List<FieldIssue> d, HttpServletRequest r) {
        return ResponseEntity.status(s).body(ErrorResponse.of(error, msg, d, r.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException e, HttpServletRequest r) {
        return build(e.getStatus(), e.getError(), e.getMessage(), e.getDetails(), r);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        List<FieldIssue> d = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldIssue(f.getField(), f.getDefaultMessage())).toList();
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO_ERRO", "Dados da requisição inválidos.", d, r);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
                "Corpo da requisição ausente, malformado ou com valor não reconhecido (ex.: canalPedido inválido).", null, r);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Valor inválido para o parâmetro.",
                List.of(new FieldIssue(e.getName(), "valor inválido: " + e.getValue())), r);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParam(MissingServletRequestParameterException e, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, "PARAMETRO_OBRIGATORIO", "Parâmetro obrigatório ausente.",
                List.of(new FieldIssue(e.getParameterName(), "obrigatório")), r);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException e, HttpServletRequest r) {
        return build(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Seu perfil não tem permissão para esta operação.", null, r);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> auth(AuthenticationException e, HttpServletRequest r) {
        return build(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Autenticação necessária.", null, r);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        return build(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS", "A operação viola uma restrição de integridade dos dados.", null, r);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> method(HttpRequestMethodNotSupportedException e, HttpServletRequest r) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_SUPORTADO", "Método HTTP não suportado para esta rota.", null, r);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> noResource(NoResourceFoundException e, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, "ROTA_NAO_ENCONTRADA", "Rota não encontrada.", null, r);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> generic(Exception e, HttpServletRequest r) {
        log.error("Erro inesperado em {}", r.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Erro interno do servidor.", null, r);
    }
}
