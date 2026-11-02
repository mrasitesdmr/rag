package com.rasitbank.assistant.evaluation.controller;

import com.rasitbank.assistant.evaluation.domain.dto.EvalOverviewResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunDetailResponse;
import com.rasitbank.assistant.evaluation.domain.dto.EvalRunResponse;
import com.rasitbank.assistant.evaluation.service.EvalRunService;
import com.rasitbank.assistant.pagination.PageParameters;
import com.rasitbank.assistant.pagination.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Değerlendirme çalıştırmasını başlatır (202) ve çalıştırmaları, tek çalıştırmayı ve ekranın özetini sunar.
 */
@RestController
@RequestMapping("/api/assistant/evals")
public class EvalRunController {

    private static final String EVAL_RUN_PATH = "/api/assistant/evals/";

    private final EvalRunService evalRunService;

    public EvalRunController(EvalRunService evalRunService) {
        this.evalRunService = evalRunService;
    }

    @PostMapping
    public ResponseEntity<EvalRunResponse> startEvalRun() {
        EvalRunResponse run = evalRunService.startEvalRun();
        return ResponseEntity.accepted().location(URI.create(EVAL_RUN_PATH + run.evalRunId())).body(run);
    }

    @GetMapping
    public PageResponse<EvalRunResponse> listEvalRuns(
            @RequestParam(name = "page", defaultValue = "0")
            @Min(value = 0, message = PageParameters.PAGE_MESSAGE) int page,
            @RequestParam(name = "size", defaultValue = "20")
            @Min(value = 1, message = PageParameters.SIZE_MESSAGE)
            @Max(value = PageParameters.MAX_SIZE, message = PageParameters.SIZE_MESSAGE) int size) {
        return PageResponse.from(evalRunService.listEvalRuns(page, size));
    }

    @GetMapping("/overview")
    public EvalOverviewResponse getEvalOverview() {
        return evalRunService.getEvalOverview();
    }

    @GetMapping("/{evalRunId}")
    public EvalRunDetailResponse getEvalRun(@PathVariable long evalRunId) {
        return evalRunService.getEvalRun(evalRunId);
    }
}
