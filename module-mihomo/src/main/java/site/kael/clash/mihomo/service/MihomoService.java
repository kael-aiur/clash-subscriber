package site.kael.clash.mihomo.service;

import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.mihomo.model.HealthStatus;
import site.kael.clash.mihomo.model.MihomoInstance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mihomo 实例管理服务接口
 * <p>
 * 提供实例 CRUD、健康检查和配置推送功能。
 */
public interface MihomoService {

    /**
     * 创建 Mihomo 实例（自动生成 ID）
     */
    MihomoInstance create(MihomoInstance instance);

    /**
     * 更新已有的 Mihomo 实例
     */
    MihomoInstance update(MihomoInstance instance);

    /**
     * 根据 ID 查找实例
     */
    Optional<MihomoInstance> findById(String id);

    /**
     * 查找所有实例
     */
    List<MihomoInstance> findAll();

    /**
     * 根据 ID 删除实例
     */
    void deleteById(String id);

    /**
     * 检查单个实例的健康状态
     */
    HealthStatus checkHealth(String instanceId);

    /**
     * 检查所有已启用实例的健康状态
     *
     * @return 实例 ID 到健康状态的映射
     */
    Map<String, HealthStatus> checkHealthAll();

    /**
     * 获取指定实例的当前运行配置
     *
     * @param instanceId 实例 ID
     * @return JSON 格式的配置字符串
     */
    String getConfig(String instanceId);

    /**
     * 获取指定实例的规则列表
     *
     * @param instanceId 实例 ID
     * @return JSON 格式的规则数据
     */
    String getRules(String instanceId);

    /**
     * 获取指定实例的代理和代理组
     *
     * @param instanceId 实例 ID
     * @return JSON 格式的代理数据
     */
    String getProxies(String instanceId);

    /**
     * 向单个实例推送配置
     */
    void pushConfig(String instanceId, ClashConfig config);

    /**
     * 向所有已启用实例推送配置
     *
     * @return 实例 ID 到推送结果的映射（true=成功，false=失败）
     */
    Map<String, Boolean> pushConfigAll(ClashConfig config);
}
