package com.rasitbank.retrieval.exception;

import org.springframework.http.HttpStatus;

/**
 * API'ye dönük domain hatalarının ortak tabanıdır; sabit errorCode ve HTTP durumunu taşır.
 */
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    protected BaseException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
