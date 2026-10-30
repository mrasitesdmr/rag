package com.rasitbank.retrieval.exception;

import org.springframework.http.HttpStatus;

/**
 * İsteğin mevcut durumla çakıştığını (ör. embedding modeli farkı) bildirir; 409 döner.
 */
public class ConflictException extends BaseException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
