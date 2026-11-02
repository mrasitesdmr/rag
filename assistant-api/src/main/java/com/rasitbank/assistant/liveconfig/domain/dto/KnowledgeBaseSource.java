package com.rasitbank.assistant.liveconfig.domain.dto;

/**
 * Bilgi bankasındaki aranabilir (güncel sürümü olan) bir belgenin kimliği, adı ve güncel sürüm etiketidir.
 */
public record KnowledgeBaseSource(long sourceId, String name, String version) {
}
