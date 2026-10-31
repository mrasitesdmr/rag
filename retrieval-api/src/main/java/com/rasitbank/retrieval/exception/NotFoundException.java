package com.rasitbank.retrieval.exception;

import org.springframework.http.HttpStatus;

/**
 * İstenen kaydın (çalışma alanı, kaynak, içe alma işi) bulunamadığını bildirir; 404 döner.
 */
public class NotFoundException extends BaseException {

    public NotFoundException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
