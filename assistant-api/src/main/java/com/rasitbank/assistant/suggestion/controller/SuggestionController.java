package com.rasitbank.assistant.suggestion.controller;

import com.rasitbank.assistant.suggestion.domain.dto.ReplaceSuggestionsRequest;
import com.rasitbank.assistant.suggestion.domain.dto.SuggestionSetResponse;
import com.rasitbank.assistant.suggestion.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Önizleme'nin soru önerilerini okur ve ürün ekibinin yüklediği tam listeyle değiştirir.
 */
@RestController
@RequestMapping("/api/assistant/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public SuggestionSetResponse getSuggestions() {
        return suggestionService.getSuggestions();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public SuggestionSetResponse replaceSuggestions(@Valid @RequestBody ReplaceSuggestionsRequest request) {
        return suggestionService.replaceSuggestions(request);
    }
}
