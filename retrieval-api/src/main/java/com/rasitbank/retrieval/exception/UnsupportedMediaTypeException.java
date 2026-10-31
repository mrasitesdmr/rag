package com.rasitbank.retrieval.exception;

import org.springframework.http.HttpStatus;

/**
 * Yüklenen dosyanın türünün kabul edilmediğini (ör. PDF olmayan dosya) bildirir; 415 döner.
 */
public class UnsupportedMediaTypeException extends BaseException {

    public UnsupportedMediaTypeException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
