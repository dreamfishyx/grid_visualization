package com.dreamfish.backend.service.impl;

import com.dreamfish.backend.common.TrackedSseEmitter;
import com.dreamfish.backend.service.SSEService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Dream fish
 * @version 1.0
 * @description: sse service
 * @date 2025/4/24 16:15
 */
@Service
@Slf4j
public class SSEServiceImpl implements SSEService {

    private final CopyOnWriteArrayList<TrackedSseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public SseEmitter subscribe() {
        TrackedSseEmitter emitter = new TrackedSseEmitter(3600_000L);
        emitter.onCompletion(() -> {
            emitter.complete();
            removeEmitter(emitter);
        });
        emitter.onTimeout(() -> {
            emitter.markTimeout();
            removeEmitter(emitter);
        });
        emitters.add(emitter);
        log.info("subscribe->emitters size: {}", emitters.size());
        return emitter;
    }

    private void removeEmitter(TrackedSseEmitter emitter) {
        if (!emitter.isCompleted()) {
            try {

                emitter.complete();
            } catch (Exception e) {
                log.info("complete error: {}", e.getMessage());
            }
        }
        emitters.remove(emitter);
        log.info("removeEmitter->emitters size: {}", emitters.size());
    }

    @Async
    @Override
    public void sendDataUpdateEvent(String name, String data) {
        List<TrackedSseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                if (!emitter.isCompleted()) {
                    emitter.send(
                            SseEmitter.event()
                                    .name(name)
                                    .data(data)
                    );
                } else {
                    deadEmitters.add(emitter);
                }
//            } catch (IOException | IllegalStateException e) {
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });
        deadEmitters.forEach(this::removeEmitter);
    }

    @Scheduled(fixedRate = 30000) // 每30秒
    @Override
    public void sendHeartbeat() {
        emitters.forEach(emitter -> {
            try {
                if (!emitter.isCompleted()) {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } else {
                    removeEmitter(emitter);
                }
            } catch (IOException | IllegalStateException e) {
                removeEmitter(emitter);
            }
        });
    }
}
//@Service
//@Slf4j
//public class SSEService {
//
//    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
//
//    public SseEmitter subscribe() {
//        SseEmitter emitter = new SseEmitter(3600_000L);
//        emitter.onCompletion(() -> removeEmitter(emitter));
//        emitter.onTimeout(() -> {
//            removeEmitter(emitter); // 移除超时的连接
//        });
//        emitters.add(emitter);
//        log.info("emitters size: {}", emitters.size());
//        return emitter;
//    }
//
//    private void removeEmitter(SseEmitter emitter) {
//        emitter.complete();
//        emitters.remove(emitter);
//    }
//
//    @Async
//    public void sendDataUpdateEvent(String name, String data) {
//        List<SseEmitter> deadEmitters = new ArrayList<>();
//        emitters.forEach(emitter -> {
//            try {
//                emitter.send(
//                        SseEmitter.event()
//                                .name(name)
//                                .data(data)
//                );
//            } catch (IOException | IllegalStateException e) {
//                deadEmitters.add(emitter);
//            }
//        });
//        emitters.removeAll(deadEmitters);
//    }
//
//    @Scheduled(fixedRate = 30000) // 每30秒
//    public void sendHeartbeat() {
//        List<SseEmitter> deadEmitters = new ArrayList<>();
//        emitters.forEach(emitter -> {
//            try {
//                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
//            } catch (IOException | IllegalStateException e) {
//                deadEmitters.add(emitter);
//            }
//        });
//        emitters.removeAll(deadEmitters);
//    }
//}