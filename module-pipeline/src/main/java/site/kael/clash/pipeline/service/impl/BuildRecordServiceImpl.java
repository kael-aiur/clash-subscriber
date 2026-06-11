package site.kael.clash.pipeline.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildProgressEvent;
import site.kael.clash.pipeline.service.BuildRecordService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 构建记录服务实现，管理 SSE 订阅
 */
@Service
public class BuildRecordServiceImpl implements BuildRecordService {

    private static final Logger log = LoggerFactory.getLogger(BuildRecordServiceImpl.class);

    /**
     * 订阅者映射：recordId -> List<SseEmitter>
     */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 事件缓冲：recordId -> List<BuildProgressEvent>
     * 用于缓存在订阅者连接之前发送的事件，连接后重放
     */
    private final Map<String, List<BuildProgressEvent>> eventBuffer = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribeProgress(String recordId) {
        // 创建不限时的 SSE 发射器
        SseEmitter emitter = new SseEmitter(0L);

        // 添加到订阅列表
        emitters.computeIfAbsent(recordId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("新增 SSE 订阅: recordId={}, 当前订阅数: {}", recordId,
                emitters.get(recordId).size());

        // 注册回调：完成、超时、错误时移除
        emitter.onCompletion(() -> removeEmitter(recordId, emitter));
        emitter.onTimeout(() -> removeEmitter(recordId, emitter));
        emitter.onError(e -> removeEmitter(recordId, emitter));

        // 重放缓存的事件
        replayBufferedEvents(recordId, emitter);

        return emitter;
    }

    @Override
    public void publishEvent(String recordId, BuildProgressEvent event) {
        List<SseEmitter> emitterList = emitters.get(recordId);
        if (emitterList == null || emitterList.isEmpty()) {
            // 没有订阅者，缓存事件以便后续重放
            log.debug("无订阅者，缓冲事件: recordId={}, type={}, stepName={}",
                    recordId, event.getType(), event.getStepName());
            eventBuffer.computeIfAbsent(recordId, k -> new ArrayList<>()).add(event);
            return;
        }

        log.debug("发布进度事件: recordId={}, type={}, stepName={}",
                recordId, event.getType(), event.getStepName());

        for (SseEmitter emitter : emitterList) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType().name().toLowerCase().replace('_', '-'))
                        .data(event));
            } catch (IOException e) {
                log.warn("发送 SSE 事件失败: {}", e.getMessage());
                removeEmitter(recordId, emitter);
            }
        }

        // 如果是终态事件，清理缓冲
        if (event.getType() == BuildProgressEvent.EventType.BUILD_COMPLETE
                || event.getType() == BuildProgressEvent.EventType.BUILD_ERROR) {
            eventBuffer.remove(recordId);
        }
    }

    /**
     * 重放缓存的事件给新连接的订阅者
     */
    private void replayBufferedEvents(String recordId, SseEmitter emitter) {
        List<BuildProgressEvent> buffered = eventBuffer.get(recordId);
        if (buffered == null || buffered.isEmpty()) {
            return;
        }

        log.debug("重放缓存事件: recordId={}, 事件数: {}", recordId, buffered.size());

        for (BuildProgressEvent event : buffered) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType().name().toLowerCase().replace('_', '-'))
                        .data(event));
            } catch (IOException e) {
                log.warn("重放 SSE 事件失败: {}", e.getMessage());
                removeEmitter(recordId, emitter);
                break;
            }
        }
    }

    /**
     * 移除订阅者
     */
    private void removeEmitter(String recordId, SseEmitter emitter) {
        List<SseEmitter> emitterList = emitters.get(recordId);
        if (emitterList != null) {
            emitterList.remove(emitter);
            log.debug("移除 SSE 订阅: recordId={}, 剩余订阅数: {}", recordId, emitterList.size());
            if (emitterList.isEmpty()) {
                emitters.remove(recordId);
            }
        }
    }
}
