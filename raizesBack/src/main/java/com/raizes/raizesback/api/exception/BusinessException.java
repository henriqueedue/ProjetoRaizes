package com.raizes.raizesback.api.exception;

import com.raizes.raizesback.api.dto.Dtos.FieldIssue;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String error;
    private final List<FieldIssue> details;

    public BusinessException(HttpStatus status, String error, String message, List<FieldIssue> details) {
        super(message);
        this.status = status;
        this.error = error;
        this.details = details == null ? List.of() : details;
    }

    public BusinessException(HttpStatus status, String error, String message) {
        this(status, error, message, List.of());
    }

    public static BusinessException naoEncontrado(String error, String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, error, message);
    }

    public static BusinessException conflito(String error, String message) {
        return new BusinessException(HttpStatus.CONFLICT, error, message);
    }

    public static BusinessException proibido(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", message);
    }
}
