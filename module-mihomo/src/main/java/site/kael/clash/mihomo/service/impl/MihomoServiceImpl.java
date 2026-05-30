package site.kael.clash.mihomo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.util.IdGenerator;
import site.kael.clash.mihomo.client.MihomoHttpClient;
import site.kael.clash.mihomo.model.HealthStatus;
import site.kael.clash.mihomo.model.MihomoInstance;
import site.kael.clash.mihomo.repository.MihomoInstanceRepository;
import site.kael.clash.mihomo.service.MihomoService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mihomo 实例管理服务实现
 */
@Service
public class MihomoServiceImpl implements MihomoService {

    private static final Logger log = LoggerFactory.getLogger(MihomoServiceImpl.class);

    private final MihomoInstanceRepository repository;
    private final MihomoHttpClient httpClient;
    private final Yaml yaml;

    public MihomoServiceImpl(MihomoInstanceRepository repository, MihomoHttpClient httpClient) {
        this.repository = repository;
        this.httpClient = httpClient;
        DumperOptions options = new DumperOptions();
        options.setWidth(Integer.MAX_VALUE);
        this.yaml = new Yaml(options);
    }

    @Override
    public MihomoInstance create(MihomoInstance instance) {
        instance.setId(IdGenerator.generate());
        log.info("创建 Mihomo 实例: id={}, name={}", instance.getId(), instance.getName());
        return repository.save(instance);
    }

    @Override
    public MihomoInstance update(MihomoInstance instance) {
        if (instance.getId() == null) {
            throw new BusinessException("更新实例时 ID 不能为空");
        }
        // 确保实例已存在
        repository.findById(instance.getId())
                .orElseThrow(() -> new BusinessException("实例不存在: " + instance.getId()));
        log.info("更新 Mihomo 实例: id={}", instance.getId());
        return repository.save(instance);
    }

    @Override
    public Optional<MihomoInstance> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<MihomoInstance> findAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(String id) {
        log.info("删除 Mihomo 实例: id={}", id);
        repository.deleteById(id);
    }

    @Override
    public HealthStatus checkHealth(String instanceId) {
        MihomoInstance instance = repository.findById(instanceId)
                .orElseThrow(() -> new BusinessException("实例不存在: " + instanceId));

        boolean healthy = httpClient.checkHealth(instance.getApiUrl(), instance.getApiSecret());
        HealthStatus status = healthy ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;

        instance.setStatus(status);
        instance.setLastHealthCheck(LocalDateTime.now());
        repository.save(instance);

        log.info("健康检查完成: id={}, status={}", instanceId, status);
        return status;
    }

    @Override
    public Map<String, HealthStatus> checkHealthAll() {
        Map<String, HealthStatus> results = new HashMap<>();
        List<MihomoInstance> instances = repository.findAll();

        for (MihomoInstance instance : instances) {
            if (!instance.isEnabled()) {
                continue;
            }
            try {
                HealthStatus status = checkHealth(instance.getId());
                results.put(instance.getId(), status);
            } catch (Exception e) {
                log.error("健康检查失败: id={}", instance.getId(), e);
                results.put(instance.getId(), HealthStatus.UNHEALTHY);
            }
        }

        return results;
    }

    @Override
    public String getConfig(String instanceId) {
        MihomoInstance instance = repository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(404, "Mihomo 实例不存在: " + instanceId));
        return httpClient.getConfig(instance.getApiUrl(), instance.getApiSecret());
    }

    @Override
    public String getRules(String instanceId) {
        MihomoInstance instance = repository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(404, "Mihomo 实例不存在: " + instanceId));
        return httpClient.getRules(instance.getApiUrl(), instance.getApiSecret());
    }

    @Override
    public String getProxies(String instanceId) {
        MihomoInstance instance = repository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(404, "Mihomo 实例不存在: " + instanceId));
        return httpClient.getProxies(instance.getApiUrl(), instance.getApiSecret());
    }

    @Override
    public void pushConfig(String instanceId, ClashConfig config) {
        MihomoInstance instance = repository.findById(instanceId)
                .orElseThrow(() -> new BusinessException("实例不存在: " + instanceId));

        String yamlConfig = yaml.dump(config.getRaw());
        log.info("推送配置到实例: id={}, name={}", instanceId, instance.getName());
        httpClient.pushConfig(instance.getApiUrl(), instance.getApiSecret(), yamlConfig);
    }

    @Override
    public Map<String, Boolean> pushConfigAll(ClashConfig config) {
        Map<String, Boolean> results = new HashMap<>();
        List<MihomoInstance> instances = repository.findAll();
        String yamlConfig = yaml.dump(config.getRaw());

        for (MihomoInstance instance : instances) {
            if (!instance.isEnabled()) {
                continue;
            }
            try {
                httpClient.pushConfig(instance.getApiUrl(), instance.getApiSecret(), yamlConfig);
                results.put(instance.getId(), true);
                log.info("推送配置成功: id={}", instance.getId());
            } catch (Exception e) {
                log.error("推送配置失败: id={}", instance.getId(), e);
                results.put(instance.getId(), false);
            }
        }

        return results;
    }
}
