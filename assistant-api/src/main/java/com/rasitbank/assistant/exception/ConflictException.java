package com.rasitbank.assistant.exception;

import org.springframework.http.HttpStatus;

/**
 * İsteğin mevcut durumla çakıştığını (konuşmada süren bir tur) bildirir; 409 döner.
 */
public class ConflictException extends BaseException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
