package com.rasitbank.assistant.conversation.domain.entity;

/**
 * Konuşmalar ekranında bir turun durum çipidir; saklanmaz, tur okunurken kayıtlı alanlardan hesaplanır.
 */
public enum TurnStatus {
    IN_PROGRESS,
    INCOMPLETE,
    UNGROUNDED,
    DONT_KNOW,
    WRONG,
    GROUNDED
}
