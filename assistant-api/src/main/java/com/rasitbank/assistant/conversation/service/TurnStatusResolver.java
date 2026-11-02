package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Kayıtlı bir turun durumunu onaylanan kural sırasıyla hesaplar: hata, Sadece LLM, "bulamadım" cevabı (golden set'teki
 * cevap parçası prompt'a girdi mi, girmedi mi, cevap parçası yok mu), "Hayır" geri bildirimi ve alıntı varlığı.
 */
@Component
public class TurnStatusResolver {

    public TurnStatus resolve(Turn turn, Set<Long> answerChunkIds) {
        if (turn.getErrorCode() != null) {
            return TurnStatus.INCOMPLETE;
        }
        if (turn.getGenerationMode() == GenerationMode.LLM_ONLY) {
            return TurnStatus.UNGROUNDED;
        }
        if (turn.isNotFoundAnswer()) {
            return notFoundStatus(turn, answerChunkIds);
        }
        if (Boolean.FALSE.equals(turn.getHelpful())) {
            return TurnStatus.WRONG;
        }
        if (!turn.getCited().isEmpty()) {
            return TurnStatus.GROUNDED;
        }
        return TurnStatus.UNGROUNDED;
    }

    private static TurnStatus notFoundStatus(Turn turn, Set<Long> answerChunkIds) {
        if (answerChunkIds.isEmpty()) {
            return TurnStatus.DONT_KNOW;
        }
        boolean isAnswerChunkInPrompt = turn.getChunks().stream()
                .anyMatch(chunk -> chunk.getPromptPosition() != null && answerChunkIds.contains(chunk.getChunkId()));
        return isAnswerChunkInPrompt ? TurnStatus.GENERATION_ERROR : TurnStatus.MISSED;
    }
}
