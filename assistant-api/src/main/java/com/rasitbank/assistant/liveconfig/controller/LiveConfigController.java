package com.rasitbank.assistant.liveconfig.controller;

import com.rasitbank.assistant.liveconfig.domain.dto.LiveConfigResponse;
import com.rasitbank.assistant.liveconfig.service.LiveConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Önizleme'nin yapılandırma şeridi için asistanın etkin yapılandırmasını salt okunur olarak verir.
 */
@RestController
public class LiveConfigController {

    private final LiveConfigService liveConfigService;

    public LiveConfigController(LiveConfigService liveConfigService) {
        this.liveConfigService = liveConfigService;
    }

    @GetMapping("/api/assistant/live-config")
    public LiveConfigResponse getLiveConfig() {
        return liveConfigService.getLiveConfig();
    }
}
