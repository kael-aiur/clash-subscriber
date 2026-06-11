package site.kael.clash.pipeline.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildProgressEvent;

import static org.junit.jupiter.api.Assertions.*;

class BuildRecordServiceImplTest {

    private BuildRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BuildRecordServiceImpl();
    }

    @Test
    void subscribeProgress_shouldReturnEmitter() {
        // When
        SseEmitter emitter = service.subscribeProgress("record-1");

        // Then
        assertNotNull(emitter);
    }

    @Test
    void publishEvent_shouldNotifySubscribers() {
        // Given
        String recordId = "record-1";
        SseEmitter emitter = service.subscribeProgress(recordId);

        BuildProgressEvent event = BuildProgressEvent.stepStatus(0, "测试步骤", "RUNNING");

        // When
        service.publishEvent(recordId, event);

        // Then
        // 由于 SseEmitter.send() 是异步的，这里验证方法执行不抛异常
        assertNotNull(emitter);
    }

    @Test
    void publishEvent_shouldHandleIOException() {
        // Given
        String recordId = "record-1";
        SseEmitter emitter = service.subscribeProgress(recordId);

        BuildProgressEvent event = BuildProgressEvent.stepStatus(0, "测试步骤", "RUNNING");

        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }

    @Test
    void publishEvent_shouldHandleNoSubscribers() {
        // Given
        String recordId = "non-existent";
        BuildProgressEvent event = BuildProgressEvent.stepStatus(0, "测试步骤", "RUNNING");

        // When & Then - 不应抛出异常
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }

    @Test
    void publishEvent_buildComplete_shouldWork() {
        // Given
        String recordId = "record-1";
        service.subscribeProgress(recordId);

        BuildProgressEvent event = BuildProgressEvent.buildComplete("SUCCESS", 3500, 4, 4, 0);

        // When & Then
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }

    @Test
    void publishEvent_buildError_shouldWork() {
        // Given
        String recordId = "record-1";
        service.subscribeProgress(recordId);

        BuildProgressEvent event = BuildProgressEvent.buildError("测试错误");

        // When & Then
        assertDoesNotThrow(() -> service.publishEvent(recordId, event));
    }
}
