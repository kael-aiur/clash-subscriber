package site.kael.clash.processor.service;

import site.kael.clash.processor.model.RuleGroup;

import java.util.List;
import java.util.Optional;

/**
 * 规则组服务接口
 */
public interface RuleGroupService {

    /**
     * 查询所有规则组
     */
    List<RuleGroup> findAll();

    /**
     * 根据 ID 查询规则组
     */
    Optional<RuleGroup> findById(String id);

    /**
     * 根据来源订阅 ID 查询规则组
     */
    Optional<RuleGroup> findBySourceSubscriptionId(String subscriptionId);

    /**
     * 手动创建规则组
     */
    RuleGroup create(RuleGroup ruleGroup);

    /**
     * 更新规则组
     */
    RuleGroup update(RuleGroup ruleGroup);

    /**
     * 删除规则组
     */
    void deleteById(String id);

    /**
     * 从订阅提取规则组。
     * 若该订阅已有规则组，则完全覆盖更新。
     *
     * @param subscriptionId 订阅 ID
     * @return 提取后的规则组
     */
    RuleGroup extractFromSubscription(String subscriptionId);
}
