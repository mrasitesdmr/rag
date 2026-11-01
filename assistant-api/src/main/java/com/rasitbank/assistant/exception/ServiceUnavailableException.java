package com.rasitbank.assistant.exception;

import org.springframework.http.HttpStatus;

/**
 * Dış bir servisin (retrieval-api, reranker, dil modeli) kullanılamadığını bildirir; 503 döner.
 */
public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
