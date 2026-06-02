package com.bank.common.exception;

import lombok.Getter;

/**
 * Base business exception. Subclass per bounded context for domain errors.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
