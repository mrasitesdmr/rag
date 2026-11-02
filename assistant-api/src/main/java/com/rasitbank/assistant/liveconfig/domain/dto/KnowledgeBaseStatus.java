package com.rasitbank.assistant.liveconfig.domain.dto;

/**
 * Bilgi bankasının durumudur: aranabilir en az bir belge var, aranabilir belge yok ya da retrieval-api okunamadı.
 */
public enum KnowledgeBaseStatus {
    READY,
    EMPTY,
    UNAVAILABLE
}
