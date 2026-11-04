package com.rasitbank.assistant.generation;

/**
 * Dil modeline sorulan etiket çağrısının sonucunu taşır: bulunan etiket, çağrının süresi ve çağrının hata ya da zaman
 * aşımıyla bitip bitmediği.
 */
public record MessageLabelResult(MessageLabel label, long latencyMs, boolean failed) {
}
