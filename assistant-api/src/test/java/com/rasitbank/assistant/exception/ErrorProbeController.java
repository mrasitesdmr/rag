package com.rasitbank.assistant.exception;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hata eşlemesini sınamak için yalnızca testte bulunan, bilerek hata üreten uçlar.
 */
@RestController
@RequestMapping("/api/assistant/probe")
class ErrorProbeController {

    record EchoRequest(@NotBlank String text) {
    }

    @GetMapping("/get-only")
    String getOnly() {
        return "ok";
    }

    @PostMapping(value = "/echo", consumes = "application/json", produces = "application/json")
    EchoRequest echo(@RequestBody @jakarta.validation.Valid EchoRequest request) {
        return request;
    }

    @GetMapping(value = "/json-only", produces = "application/json")
    EchoRequest jsonOnly() {
        return new EchoRequest("x");
    }

    @GetMapping("/page")
    String page(@RequestParam @Min(0) int page) {
        return "page " + page;
    }

    @GetMapping("/named-page")
    String namedPage(@RequestParam(name = "pageNumber") @Min(0) int page) {
        return "page " + page;
    }

    @GetMapping("/missing")
    String missing() {
        throw new NotFoundException("CONVERSATION_NOT_FOUND", "missing");
    }

    @GetMapping("/conflict")
    String conflict() {
        throw new ConflictException("TURN_IN_PROGRESS", "busy");
    }

    @GetMapping("/unavailable")
    String unavailable() {
        throw new ServiceUnavailableException("RETRIEVAL_UNAVAILABLE", "down");
    }

    @GetMapping("/boom")
    String boom() {
        throw new IllegalStateException("boom-secret");
    }
}
