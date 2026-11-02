package com.rasitbank.assistant.evaluation.controller;

import com.rasitbank.assistant.evaluation.domain.dto.GoldenSetSummaryResponse;
import com.rasitbank.assistant.evaluation.domain.dto.ReplaceGoldenSetRequest;
import com.rasitbank.assistant.evaluation.service.GoldenSetService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kayıtlı golden set'in özetini okur ve ürün ekibinin yüklediği tam setle değiştirir.
 */
@RestController
@RequestMapping("/api/assistant/golden-set")
public class GoldenSetController {

    private final GoldenSetService goldenSetService;

    public GoldenSetController(GoldenSetService goldenSetService) {
        this.goldenSetService = goldenSetService;
    }

    @GetMapping
    public GoldenSetSummaryResponse getGoldenSet() {
        return goldenSetService.getGoldenSet();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public GoldenSetSummaryResponse replaceGoldenSet(@Valid @RequestBody ReplaceGoldenSetRequest request) {
        return goldenSetService.replaceGoldenSet(request);
    }
}
