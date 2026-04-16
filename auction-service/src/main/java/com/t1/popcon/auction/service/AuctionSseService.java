package com.t1.popcon.auction.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSseService {

    private final MeterRegistry registry;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @PostConstruct
    private void registerSseTotalGauge() {
        Gauge.builder("popcon_sse_active_connections", emitters,
                        m -> m.values().stream().mapToInt(List::size).sum())
                .description("경매 SSE 총 활성 연결 수")
                .register(registry);
    }

    public SseEmitter subscribe(Long auctionId) {
        SseEmitter emitter = new SseEmitter(60L * 60 * 1000); // 1시간

        emitters.computeIfAbsent(auctionId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        registry.counter("popcon_sse_event_total",
                "auction_id", String.valueOf(auctionId), "type", "subscribe").increment();

        emitter.onCompletion(() -> {
            removeEmitter(auctionId, emitter);
            registry.counter("popcon_sse_event_total",
                    "auction_id", String.valueOf(auctionId), "type", "complete").increment();
        });
        emitter.onTimeout(() -> {
            removeEmitter(auctionId, emitter);
            registry.counter("popcon_sse_event_total",
                    "auction_id", String.valueOf(auctionId), "type", "timeout").increment();
        });
        emitter.onError(e -> {
            removeEmitter(auctionId, emitter);
            registry.counter("popcon_sse_event_total",
                    "auction_id", String.valueOf(auctionId), "type", "error").increment();
        });

        return emitter;
    }

    public void send(Long auctionId, Object payload) {
        List<SseEmitter> auctionEmitters = emitters.getOrDefault(auctionId, List.of());

        for (SseEmitter emitter : auctionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("auction-price")
                        .data(payload));
                registry.counter("popcon_sse_event_total",
                        "auction_id", String.valueOf(auctionId), "type", "send").increment();
            } catch (IOException e) {
                log.warn("SSE 전송 실패 - auctionId={}", auctionId, e);
                removeEmitter(auctionId, emitter);
            }
        }
    }

    public void sendHeartbeat(Long auctionId) {
        List<SseEmitter> auctionEmitters = emitters.getOrDefault(auctionId, List.of());

        for (SseEmitter emitter : auctionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("keep-alive"));
                registry.counter("popcon_sse_event_total",
                        "auction_id", String.valueOf(auctionId), "type", "heartbeat").increment();
            } catch (IOException e) {
                log.warn("SSE heartbeat 전송 실패 - auctionId={}", auctionId, e);
                removeEmitter(auctionId, emitter);
            }
        }
    }

    public boolean hasSubscribers(Long auctionId) {
        List<SseEmitter> auctionEmitters = emitters.get(auctionId);
        return auctionEmitters != null && !auctionEmitters.isEmpty();
    }

    private void removeEmitter(Long auctionId, SseEmitter emitter) {
        List<SseEmitter> auctionEmitters = emitters.get(auctionId);
        if (auctionEmitters != null) {
            auctionEmitters.remove(emitter);
            if (auctionEmitters.isEmpty()) {
                emitters.remove(auctionId);
            }
        }
    }
}