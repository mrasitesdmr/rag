package com.rasitbank.assistant.conversation.service;

import java.time.Instant;

/**
 * Cevabı hâlâ üretilen turun bellekteki anlık görüntüsüdür: müşterinin sorusu ve sorunun alındığı an.
 */
public record ActiveTurn(String question, Instant startedAt) {
}
