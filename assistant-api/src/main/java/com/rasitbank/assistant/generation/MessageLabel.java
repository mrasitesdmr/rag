package com.rasitbank.assistant.generation;

/**
 * Müşteri mesajının selamlaşma türünü taşır: selam, teşekkür, veda ya da bunların dışındaki her şey; hazır karşılık
 * metni bu türe göre seçilir.
 */
public enum MessageLabel {
    GREETING,
    THANKS,
    FAREWELL,
    OTHER;

    public boolean isSmallTalk() {
        return this != OTHER;
    }
}
