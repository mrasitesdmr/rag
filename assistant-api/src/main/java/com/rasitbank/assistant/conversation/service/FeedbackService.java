package com.rasitbank.assistant.conversation.service;

import com.rasitbank.assistant.conversation.domain.dto.SubmitFeedbackRequest;
import com.rasitbank.assistant.conversation.domain.entity.Turn;
import com.rasitbank.assistant.conversation.repository.TurnRepository;
import com.rasitbank.assistant.exception.ConflictException;
import com.rasitbank.assistant.exception.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tamamlanmış bir tura müşterinin geri bildirimini yazar; son gelen seçim öncekinin yerine geçer.
 */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final TurnRepository turnRepository;
    private final Clock clock;

    public FeedbackService(TurnRepository turnRepository, Clock clock) {
        this.turnRepository = turnRepository;
        this.clock = clock;
    }

    @Transactional
    public void submitFeedback(long turnId, SubmitFeedbackRequest request) {
        Turn turn = turnRepository.findById(turnId)
                .orElseThrow(() -> new NotFoundException("TURN_NOT_FOUND", "Turn not found: " + turnId));
        if (turn.getErrorCode() != null) {
            throw new ConflictException("TURN_NOT_COMPLETED", "Turn is not completed: " + turnId);
        }
        boolean helpful = Objects.requireNonNull(request.helpful());
        turnRepository.updateFeedback(turnId, helpful, Instant.now(clock));
        log.info("Feedback submitted turnId={} helpful={}", turnId, helpful);
    }
}
