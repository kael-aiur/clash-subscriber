package site.kael.clash.subscription.service;

import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.subscription.model.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * 订阅源服务接口，提供订阅的 CRUD 及远程获取、解析能力。
 */
public interface SubscriptionService {

    /**
     * 创建订阅源，自动生成 ID 和创建时间。
     *
     * @param subscription 待创建的订阅源（id 字段会被覆盖）
     * @return 持久化后的订阅源
     */
    Subscription create(Subscription subscription);

    /**
     * 更新已有订阅源。
     *
     * @param subscription 包含更新数据的订阅源（id 必须存在）
     * @return 更新后的订阅源
     */
    Subscription update(Subscription subscription);

    /**
     * 按 ID 查询订阅源。
     */
    Optional<Subscription> findById(String id);

    /**
     * 查询全部订阅源。
     */
    List<Subscription> findAll();

    /**
     * 按 ID 删除订阅源，同时清除缓存文件。
     */
    void deleteById(String id);

    /**
     * 远程获取并解析订阅源配置。
     * <p>
     * 流程：HTTP 请求 -> 自动检测 YAML / Base64 -> 解析为 ClashConfig。
     * 若请求失败则尝试从本地缓存加载。
     *
     * @param subscriptionId 订阅源 ID
     * @return 解析后的 ClashConfig
     */
    ClashConfig fetch(String subscriptionId);
}
