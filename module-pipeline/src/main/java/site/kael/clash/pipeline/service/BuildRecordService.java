package site.kael.clash.pipeline.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import site.kael.clash.pipeline.model.BuildProgressEvent;

/**
 * 构建记录服务，提供 SSE 订阅管理
 */
public interface BuildRecordService {

    /**
     * 订阅构建进度
     *
     * @param recordId 构建记录 ID
     * @return SSE 发射器
     */
    SseEmitter subscribeProgress(String recordId);

    /**
     * 发布进度事件
     *
     * @param recordId 构建记录 ID
     * @param event    进度事件
     */
    void publishEvent(String recordId, BuildProgressEvent event);
}
