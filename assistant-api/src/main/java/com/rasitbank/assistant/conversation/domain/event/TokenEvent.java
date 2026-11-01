package com.rasitbank.assistant.conversation.domain.event;

/**
 * Cevap akışının "token" olayıdır: cevabın bir sonraki metin parçası.
 */
public record TokenEvent(String text) {
}
