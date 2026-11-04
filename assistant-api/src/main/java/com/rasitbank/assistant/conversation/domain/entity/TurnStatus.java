package com.rasitbank.assistant.conversation.domain.entity;

/**
 * Konuşmalar ekranında bir turun durum çipidir; saklanmaz, tur okunurken kayıtlı alanlardan hesaplanır.
 */
public enum TurnStatus {
    IN_PROGRESS,
    INCOMPLETE,
    UNGROUNDED,
    SMALL_TALK,
    DONT_KNOW,
    MISSED,
    GENERATION_ERROR,
    WRONG,
    GROUNDED
}
