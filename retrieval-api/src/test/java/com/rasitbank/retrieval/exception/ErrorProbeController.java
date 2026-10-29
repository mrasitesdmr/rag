package com.rasitbank.retrieval.exception;

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
@RequestMapping("/api/workspaces/probe-ws/probe")
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

    @GetMapping("/boom")
    String boom() {
        throw new IllegalStateException("boom-secret");
    }
}
