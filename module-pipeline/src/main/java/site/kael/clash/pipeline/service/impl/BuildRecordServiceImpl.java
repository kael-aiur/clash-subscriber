package site.kael.clash.pipeline.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildProgressEvent;
import site.kael.clash.pipeline.service.BuildRecordService;

import java.io.IOException;
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

        return emitter;
    }

    @Override
    public void publishEvent(String recordId, BuildProgressEvent event) {
        List<SseEmitter> emitterList = emitters.get(recordId);
        if (emitterList == null || emitterList.isEmpty()) {
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
