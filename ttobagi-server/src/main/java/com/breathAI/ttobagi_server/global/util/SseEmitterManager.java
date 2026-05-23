package com.breathAI.ttobagi_server.global.util;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterManager {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(Long analysisId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(analysisId, emitter);
        emitter.onCompletion(() -> emitters.remove(analysisId));
        emitter.onTimeout(() -> emitters.remove(analysisId));
        return emitter;
    }

    public void send(Long analysisId, String status, String message) {
        SseEmitter emitter = emitters.get(analysisId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data("{\"status\":\"" + status + "\",\"message\":\"" + message + "\"}"));
            if ("COMPLETED".equals(status) || "FAIL".equals(status)) {
                emitter.complete();
            }
        } catch (Exception e) {
            emitters.remove(analysisId);
        }
    }
}