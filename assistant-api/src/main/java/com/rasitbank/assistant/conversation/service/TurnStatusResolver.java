package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.entity.GenerationMode;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.domain.entity.TurnStatus;
import org.springframework.stereotype.Component;

/**
 * Kayıtlı bir turun durumunu onaylanan kural sırasıyla hesaplar: hata, Sadece LLM, "bulamadım" cevabı, "Hayır" geri
 * bildirimi ve alıntı varlığı.
 */
@Component
public class TurnStatusResolver {

    public TurnStatus resolve(Turn turn) {
        if (turn.getErrorCode() != null) {
            return TurnStatus.INCOMPLETE;
        }
        if (turn.getGenerationMode() == GenerationMode.LLM_ONLY) {
            return TurnStatus.UNGROUNDED;
        }
        if (turn.isNotFoundAnswer()) {
            return TurnStatus.DONT_KNOW;
        }
        if (Boolean.FALSE.equals(turn.getHelpful())) {
            return TurnStatus.WRONG;
        }
        if (!turn.getCited().isEmpty()) {
            return TurnStatus.GROUNDED;
        }
        return TurnStatus.UNGROUNDED;
    }
}
