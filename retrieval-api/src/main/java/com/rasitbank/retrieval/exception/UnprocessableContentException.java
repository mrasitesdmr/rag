package com.rasitbank.retrieval.exception;

import org.springframework.http.HttpStatus;

/**
 * Biçimce geçerli ama işlenemeyen içeriği (okunamayan ya da bölümsüz PDF) bildirir; 422 döner.
 */
public class UnprocessableContentException extends BaseException {

    public UnprocessableContentException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
