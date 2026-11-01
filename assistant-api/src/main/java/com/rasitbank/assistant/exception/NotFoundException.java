package com.rasitbank.assistant.exception;

import org.springframework.http.HttpStatus;

/**
 * İstenen kaydın (konuşma) bulunamadığını bildirir; 404 döner.
 */
public class NotFoundException extends BaseException {

    public NotFoundException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
