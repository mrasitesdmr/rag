package com.rasitbank.assistant.generation;

/**
 * Konuşma geçmişindeki cevabı tamamlanmış bir turun sorusu ve alıntı numaraları silinmiş cevabıdır; yeniden yazmaya
 * ve cevap prompt'una girer.
 */
public record HistoryTurn(String question, String answer) {
}
