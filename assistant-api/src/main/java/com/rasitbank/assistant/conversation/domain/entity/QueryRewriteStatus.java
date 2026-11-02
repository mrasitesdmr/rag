package com.rasitbank.assistant.conversation.domain.entity;

/**
 * Bir turda takip sorusunu yeniden yazma adımının sonucudur; arama sorgusunun nereden geldiğini gösterir.
 */
public enum QueryRewriteStatus {
    DISABLED,
    NO_HISTORY,
    REWRITTEN,
    FAILED,
    EMPTY
}
