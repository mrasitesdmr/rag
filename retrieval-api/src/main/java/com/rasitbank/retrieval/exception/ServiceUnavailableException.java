package com.rasitbank.retrieval.exception;

import org.springframework.http.HttpStatus;

/**
 * Dış bir servisin (embedding modeli) kullanılamadığını ya da hatalı cevap verdiğini bildirir; 503 döner.
 */
public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
