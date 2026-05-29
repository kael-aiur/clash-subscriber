package site.kael.clash.processor.service;

import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.api.ProcessingContext;
import site.kael.clash.processor.model.PipelineConfig;

/**
 * Pipeline 执行引擎接口
 */
public interface PipelineService {

    /**
     * 执行 Pipeline，使用默认的 ProcessingContext
     *
     * @param pipeline Pipeline 配置
     * @param input    输入的 Clash 配置
     * @return 处理后的 Clash 配置
     */
    ClashConfig execute(PipelineConfig pipeline, ClashConfig input);

    /**
     * 执行 Pipeline，使用自定义的 ProcessingContext
     *
     * @param pipeline Pipeline 配置
     * @param input    输入的 Clash 配置
     * @param context  自定义的处理上下文
     * @return 处理后的 Clash 配置
     */
    ClashConfig execute(PipelineConfig pipeline, ClashConfig input, ProcessingContext context);
}
