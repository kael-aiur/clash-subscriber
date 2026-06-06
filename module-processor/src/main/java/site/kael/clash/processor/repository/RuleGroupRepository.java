package site.kael.clash.processor.repository;

import site.kael.clash.processor.model.RuleGroup;

import java.util.List;
import java.util.Optional;

/**
 * 规则组仓储接口
 */
public interface RuleGroupRepository {

    /**
     * 保存规则组
     *
     * @param ruleGroup 规则组
     * @return 保存后的规则组
     */
    RuleGroup save(RuleGroup ruleGroup);

    /**
     * 根据 ID 查找规则组
     *
     * @param id 规则组 ID
     * @return 规则组
     */
    Optional<RuleGroup> findById(String id);

    /**
     * 根据来源订阅 ID 查找规则组
     *
     * @param subscriptionId 订阅 ID
     * @return 规则组（一个订阅对应一个规则组）
     */
    Optional<RuleGroup> findBySourceSubscriptionId(String subscriptionId);

    /**
     * 查找所有规则组
     *
     * @return 所有规则组列表
     */
    List<RuleGroup> findAll();

    /**
     * 根据 ID 删除规则组
     *
     * @param id 规则组 ID
     */
    void deleteById(String id);
}
