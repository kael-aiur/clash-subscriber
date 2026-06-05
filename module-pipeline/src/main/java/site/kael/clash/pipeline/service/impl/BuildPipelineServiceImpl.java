package site.kael.clash.pipeline.service.impl;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.common.util.IdGenerator;
import site.kael.clash.mihomo.service.MihomoService;
import site.kael.clash.pipeline.model.BuildPipeline;
import site.kael.clash.pipeline.model.BuildRecord;
import site.kael.clash.pipeline.model.BuildStep;
import site.kael.clash.pipeline.repository.BuildPipelineRepository;
import site.kael.clash.pipeline.repository.BuildRecordRepository;
import site.kael.clash.pipeline.service.BuildPipelineService;
import site.kael.clash.processor.api.ProcessingContext;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.model.PipelineStep;
import site.kael.clash.processor.service.PipelineService;
import site.kael.clash.scheduler.service.SchedulerService;
import site.kael.clash.subscription.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BuildPipelineServiceImpl implements BuildPipelineService {

    private static final Logger log = LoggerFactory.getLogger(BuildPipelineServiceImpl.class);

    private final BuildPipelineRepository pipelineRepository;
    private final BuildRecordRepository recordRepository;
    private final SubscriptionService subscriptionService;
    private final PipelineService pipelineService;
    private final MihomoService mihomoService;
    private final SchedulerService schedulerService;

    public BuildPipelineServiceImpl(
            BuildPipelineRepository pipelineRepository,
            BuildRecordRepository recordRepository,
            SubscriptionService subscriptionService,
            PipelineService pipelineService,
            MihomoService mihomoService,
            SchedulerService schedulerService) {
        this.pipelineRepository = pipelineRepository;
        this.recordRepository = recordRepository;
        this.subscriptionService = subscriptionService;
        this.pipelineService = pipelineService;
        this.mihomoService = mihomoService;
        this.schedulerService = schedulerService;
    }

    @PostConstruct
    public void init() {
        log.info("初始化构建流程定时调度...");
        for (BuildPipeline pipeline : pipelineRepository.findAll()) {
            if (pipeline.isEnabled() && pipeline.getCronExpression() != null && !pipeline.getCronExpression().isBlank()) {
                try {
                    schedulerService.registerBuildPipelineCron(
                            pipeline.getId(), pipeline.getCronExpression(),
                            () -> execute(pipeline.getId()));
                    log.info("已恢复构建流程定时: {} ({})", pipeline.getName(), pipeline.getId());
                } catch (Exception e) {
                    log.error("恢复构建流程定时失败: {} ({}), 原因: {}", pipeline.getName(), pipeline.getId(), e.getMessage());
                }
            }
        }
    }

    // ========== CRUD ==========

    @Override
    public BuildPipeline create(BuildPipeline pipeline) {
        pipeline.setId(IdGenerator.generate());
        LocalDateTime now = LocalDateTime.now();
        pipeline.setCreatedAt(now);
        pipeline.setUpdatedAt(now);
        BuildPipeline saved = pipelineRepository.save(pipeline);
        syncCron(saved);
        log.info("创建构建流程: {} ({})", saved.getName(), saved.getId());
        return saved;
    }

    @Override
    public BuildPipeline update(BuildPipeline pipeline) {
        if (pipeline.getId() == null || pipeline.getId().isBlank()) {
            throw new BusinessException("构建流程 ID 不能为空");
        }
        pipelineRepository.findById(pipeline.getId())
                .orElseThrow(() -> new BusinessException("构建流程不存在: " + pipeline.getId()));
        pipeline.setUpdatedAt(LocalDateTime.now());
        BuildPipeline saved = pipelineRepository.save(pipeline);
        syncCron(saved);
        log.info("更新构建流程: {} ({})", saved.getName(), saved.getId());
        return saved;
    }

    @Override
    public BuildPipeline findById(String id) {
        return pipelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "构建流程不存在: " + id));
    }

    @Override
    public List<BuildPipeline> findAll() {
        return pipelineRepository.findAll();
    }

    @Override
    public void deleteById(String id) {
        schedulerService.cancelBuildPipelineCron(id);
        pipelineRepository.deleteById(id);
        log.info("删除构建流程: {}", id);
    }

    private void syncCron(BuildPipeline pipeline) {
        schedulerService.cancelBuildPipelineCron(pipeline.getId());
        if (pipeline.isEnabled() && pipeline.getCronExpression() != null && !pipeline.getCronExpression().isBlank()) {
            schedulerService.registerBuildPipelineCron(
                    pipeline.getId(), pipeline.getCronExpression(),
                    () -> execute(pipeline.getId()));
        }
    }

    // ========== 执行 ==========

    @Override
    public BuildRecord execute(String pipelineId) {
        BuildPipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new BusinessException(404, "构建流程不存在: " + pipelineId));

        BuildRecord record = new BuildRecord();
        record.setId(IdGenerator.generate());
        record.setBuildPipelineId(pipelineId);
        record.setStartedAt(LocalDateTime.now());
        record.setStatus("RUNNING");

        log.info("开始执行构建流程: {} ({})", pipeline.getName(), pipelineId);
        List<BuildStep> steps = record.getSteps();

        try {
            // 1. 拉取主订阅配置
            String primarySubName = subscriptionService.findById(pipeline.getPrimarySubscriptionId())
                    .map(site.kael.clash.subscription.model.Subscription::getName)
                    .orElse(pipeline.getPrimarySubscriptionId());
            BuildStep step1 = startStep("拉取主订阅配置", Map.of("subscriptionName", primarySubName));
            ClashConfig config = subscriptionService.fetch(pipeline.getPrimarySubscriptionId());
            if (config == null) {
                config = new ClashConfig("empty");
                config.setRaw(new LinkedHashMap<>());
                config.setProxies(new ArrayList<>());
            }
            List<ProxyNode> allProxies = new ArrayList<>(config.getProxies() != null ? config.getProxies() : Collections.emptyList());
            Map<String, Object> step1Output = new LinkedHashMap<>();
            step1Output.put("configSummary", buildConfigSummary(config));
            step1Output.put("configYaml", configToYaml(config));
            finishStep(step1, "SUCCESS", step1Output);
            steps.add(step1);

            // 2. 合并额外订阅节点
            Map<String, Object> step2Input = new LinkedHashMap<>();
            step2Input.put("mainConfigSummary", buildConfigSummary(config));
            step2Input.put("mainConfigYaml", configToYaml(config));
            List<Map<String, Object>> extraConfigs = new ArrayList<>();
            BuildStep step2 = startStep("合并额外订阅节点", null); // input 后续设置
            if (pipeline.getAdditionalSubscriptionIds() != null) {
                for (String subId : pipeline.getAdditionalSubscriptionIds()) {
                    try {
                        ClashConfig extra = subscriptionService.fetch(subId);
                        if (extra != null && extra.getProxies() != null) {
                            String extraName = subscriptionService.findById(subId)
                                    .map(site.kael.clash.subscription.model.Subscription::getName)
                                    .orElse(subId);
                            Map<String, Object> extraInfo = new LinkedHashMap<>();
                            extraInfo.put("subscriptionName", extraName);
                            extraInfo.put("configSummary", buildConfigSummary(extra));
                            extraInfo.put("configYaml", configToYaml(extra));
                            extraConfigs.add(extraInfo);
                            allProxies.addAll(extra.getProxies());
                            log.debug("合并额外订阅: {}，节点数: {}", subId, extra.getProxies().size());
                        }
                    } catch (Exception e) {
                        log.warn("获取额外订阅失败: {}, 原因: {}", subId, e.getMessage());
                        record.getLogs().add("WARN: 获取额外订阅失败: " + subId + " - " + e.getMessage());
                    }
                }
            }
            step2Input.put("extraConfigs", extraConfigs);
            step2.setInput(step2Input);
            config.setProxies(allProxies);
            config.getRaw().put("proxies", allProxies.stream().map(this::proxyNodeToMap).toList());
            Map<String, Object> step2Output = new LinkedHashMap<>();
            step2Output.put("configSummary", buildConfigSummary(config));
            step2Output.put("configYaml", configToYaml(config));
            finishStep(step2, "SUCCESS", step2Output);
            steps.add(step2);
            record.getLogs().add("合并节点总数: " + allProxies.size());

            // 3. 脚本处理
            if (pipeline.getScriptName() != null && !pipeline.getScriptName().isBlank()) {
                Map<String, Object> step3Input = new LinkedHashMap<>();
                step3Input.put("scriptName", pipeline.getScriptName());
                step3Input.put("configSummary", buildConfigSummary(config));
                step3Input.put("configYaml", configToYaml(config));
                BuildStep step3 = startStep("脚本处理", step3Input);
                PipelineConfig pipelineConfig = new PipelineConfig();
                pipelineConfig.setId("auto-" + pipelineId);
                pipelineConfig.setName("auto-generated");

                PipelineStep scriptStep = new PipelineStep();
                scriptStep.setProcessor("script");
                scriptStep.setConfig(Map.of("scriptName", pipeline.getScriptName()));
                pipelineConfig.setSteps(List.of(scriptStep));

                ProcessingContext context = new ProcessingContext();
                config = pipelineService.execute(pipelineConfig, config, context);
                record.getLogs().addAll(context.getLogs());
                Map<String, Object> step3Output = new LinkedHashMap<>();
                step3Output.put("configSummary", buildConfigSummary(config));
                step3Output.put("configYaml", configToYaml(config));
                finishStep(step3, "SUCCESS", step3Output);
                steps.add(step3);
                record.getLogs().add("脚本处理完成: " + pipeline.getScriptName());
            } else {
                BuildStep step3 = new BuildStep();
                step3.setName("脚本处理");
                step3.setStatus("SKIPPED");
                steps.add(step3);
            }

            // 4. 推送到目标 mihomo 实例
            String instanceName = mihomoService.findById(pipeline.getTargetInstanceId())
                    .map(site.kael.clash.mihomo.model.MihomoInstance::getName)
                    .orElse(pipeline.getTargetInstanceId());
            Map<String, Object> step4Input = new LinkedHashMap<>();
            step4Input.put("instanceName", instanceName);
            step4Input.put("configSummary", buildConfigSummary(config));
            step4Input.put("configYaml", configToYaml(config));
            BuildStep step4 = startStep("推送到 Mihomo", step4Input);
            syncRawFromFields(config);
            log.debug("推送 YAML 内容: {}", new org.yaml.snakeyaml.Yaml().dump(config.getRaw()));
            try {
                mihomoService.pushConfig(pipeline.getTargetInstanceId(), config);
                finishStep(step4, "SUCCESS", Map.of("success", true));
            } catch (Exception pushEx) {
                finishStep(step4, "FAILED", Map.of("success", false));
                throw pushEx;
            }
            steps.add(step4);
            record.getLogs().add("配置推送成功: " + pipeline.getTargetInstanceId());

            // 记录成功
            record.setStatus("SUCCESS");
            record.setFinishedAt(LocalDateTime.now());

            pipeline.setLastRunAt(record.getFinishedAt());
            pipeline.setLastRunStatus("SUCCESS");
            pipelineRepository.save(pipeline);

            log.info("构建流程执行成功: {} ({})", pipeline.getName(), pipelineId);

        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            record.setFinishedAt(LocalDateTime.now());
            record.getLogs().add("ERROR: " + e.getMessage());

            pipeline.setLastRunAt(record.getFinishedAt());
            pipeline.setLastRunStatus("FAILED");
            pipelineRepository.save(pipeline);

            log.error("构建流程执行失败: {} ({}), 原因: {}", pipeline.getName(), pipelineId, e.getMessage(), e);
        }

        return recordRepository.save(record);
    }

    // ========== 构建历史 ==========

    @Override
    public List<BuildRecord> findRecords(String pipelineId) {
        return recordRepository.findByBuildPipelineId(pipelineId);
    }

    @Override
    public BuildRecord findRecordById(String recordId) {
        return recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(404, "构建记录不存在: " + recordId));
    }

    /**
     * 将 ClashConfig 的字段同步到 raw map，确保 mihomo 推送时 YAML 包含最新数据
     */
    @SuppressWarnings("unchecked")
    private void syncRawFromFields(ClashConfig config) {
        Map<String, Object> raw = config.getRaw();
        if (raw == null) {
            raw = new LinkedHashMap<>();
            config.setRaw(raw);
        }
        raw.put("proxies", config.getProxies().stream().map(this::proxyNodeToMap).toList());
        // 仅当 typed 字段非空时才覆盖 raw，避免丢失订阅源原始数据
        if (config.getProxyGroups() != null && !config.getProxyGroups().isEmpty()) {
            // Mihomo 期望 proxy-groups 为 Array 格式，需从 Map 转换
            raw.put("proxy-groups", mapToGroupArray(config.getProxyGroups()));
        }
        if (config.getRules() != null && !config.getRules().isEmpty()) {
            raw.put("rules", config.getRules());
        }
    }

    /**
     * 将 proxy-groups Map 转为 Mihomo 期望的 Array 格式
     * Map: {"组名": {type, proxies, ...}} → Array: [{name, type, proxies, ...}]
     */
    private List<Map<String, Object>> mapToGroupArray(Map<String, Object> proxyGroups) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : proxyGroups.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> groupMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> group = new LinkedHashMap<>((Map<String, Object>) groupMap);
                group.put("name", entry.getKey());
                list.add(group);
            }
        }
        return list;
    }

    private BuildStep startStep(String name, Object input) {
        BuildStep step = new BuildStep();
        step.setName(name);
        step.setStartedAt(LocalDateTime.now());
        step.setInput(input);
        return step;
    }

    private void finishStep(BuildStep step, String status, Object output) {
        step.setStatus(status);
        step.setFinishedAt(LocalDateTime.now());
        step.setOutput(output);
    }

    private Map<String, Object> buildConfigSummary(ClashConfig config) {
        List<ProxyNode> proxies = config.getProxies() != null ? config.getProxies() : Collections.emptyList();
        Map<String, Object> groups = config.getProxyGroups() != null ? config.getProxyGroups() : Collections.emptyMap();
        List<Object> rules = config.getRules() != null ? config.getRules() : Collections.emptyList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nodeCount", proxies.size());
        summary.put("proxyGroupCount", groups.size());
        summary.put("ruleCount", rules.size());
        summary.put("nodeNames", proxies.stream().limit(5).map(ProxyNode::getName).collect(Collectors.toList()));
        summary.put("proxyGroupNames", groups.keySet().stream().limit(5).collect(Collectors.toList()));
        return summary;
    }

    private String configToYaml(ClashConfig config) {
        syncRawFromFields(config);
        return new org.yaml.snakeyaml.Yaml().dump(config.getRaw());
    }

    private Map<String, Object> proxyNodeToMap(ProxyNode node) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", node.getName());
        map.put("type", node.getType());
        map.put("server", node.getServer());
        map.put("port", node.getPort());
        if (node.getExtra() != null) {
            map.putAll(node.getExtra());
        }
        return map;
    }
}
