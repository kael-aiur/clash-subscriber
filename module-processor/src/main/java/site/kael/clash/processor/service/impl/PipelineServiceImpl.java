package site.kael.clash.processor.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.api.ConfigProcessor;
import site.kael.clash.processor.api.ProcessingContext;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.model.PipelineStep;
import site.kael.clash.processor.service.PipelineService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pipeline 执行引擎实现
 */
@Service
public class PipelineServiceImpl implements PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineServiceImpl.class);

    private final Map<String, ConfigProcessor> processorMap;

    public PipelineServiceImpl(List<ConfigProcessor> processors) {
        this.processorMap = processors.stream()
                .collect(Collectors.toMap(ConfigProcessor::getName, Function.identity()));
        log.info("已注册 {} 个处理器: {}", processorMap.size(), processorMap.keySet());
    }

    @Override
    public ClashConfig execute(PipelineConfig pipeline, ClashConfig input) {
        return execute(pipeline, input, new ProcessingContext());
    }

    @Override
    public ClashConfig execute(PipelineConfig pipeline, ClashConfig input, ProcessingContext context) {
        List<PipelineStep> steps = pipeline.getSteps();
        if (steps == null || steps.isEmpty()) {
            log.debug("Pipeline [{}] 没有配置任何步骤，直接返回输入", pipeline.getName());
            return input;
        }

        log.info("开始执行 Pipeline [{}]，共 {} 个步骤", pipeline.getName(), steps.size());
        long pipelineStart = System.currentTimeMillis();

        ClashConfig current = input;
        for (int i = 0; i < steps.size(); i++) {
            PipelineStep step = steps.get(i);
            String processorName = step.getProcessor();

            // 查找处理器
            ConfigProcessor processor = processorMap.get(processorName);
            if (processor == null) {
                throw new BusinessException("处理器不存在: " + processorName);
            }

            // 将步骤配置合并到上下文变量中
            if (step.getConfig() != null) {
                for (Map.Entry<String, Object> entry : step.getConfig().entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
            }

            // 执行处理器
            log.debug("执行步骤 {}/{}: 处理器 [{}]", i + 1, steps.size(), processorName);
            long stepStart = System.currentTimeMillis();
            current = processor.process(current, context);
            long stepElapsed = System.currentTimeMillis() - stepStart;

            log.debug("步骤 {}/{} 完成: 处理器 [{}]，耗时 {}ms", i + 1, steps.size(), processorName, stepElapsed);
            context.addLog(String.format("步骤 %d/%d [%s] 完成，耗时 %dms", i + 1, steps.size(), processorName, stepElapsed));
        }

        long totalElapsed = System.currentTimeMillis() - pipelineStart;
        log.info("Pipeline [{}] 执行完成，共 {} 个步骤，总耗时 {}ms", pipeline.getName(), steps.size(), totalElapsed);
        context.addLog(String.format("Pipeline [%s] 执行完成，总耗时 %dms", pipeline.getName(), totalElapsed));

        return current;
    }
}
