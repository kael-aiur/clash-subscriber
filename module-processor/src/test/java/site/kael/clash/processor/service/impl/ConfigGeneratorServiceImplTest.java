package site.kael.clash.processor.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.builtin.NodeMergeProcessor;
import site.kael.clash.processor.builtin.ProxyGroupProcessor;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.model.NodePolicy;
import site.kael.clash.processor.model.RuleGroup;
import site.kael.clash.processor.model.SubscriptionRef;
import site.kael.clash.processor.repository.RuleGroupRepository;
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.service.SubscriptionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置生成流程测试：聚焦「订阅源节点采纳规则」是否在生成时正确生效。
 * 协作者使用手写 stub（SubscriptionService）与真实处理器（NodeMerge/ProxyGroup）。
 */
class ConfigGeneratorServiceImplTest {

    private SubscriptionService subscriptionServiceStub(ClashConfig... configs) {
        return new SubscriptionService() {
            int idx = 0;

            @Override
            public ClashConfig fetch(String subscriptionId) {
                return idx < configs.length ? configs[idx++] : null;
            }

            @Override
            public Subscription create(Subscription subscription) { throw new UnsupportedOperationException(); }

            @Override
            public Subscription update(Subscription subscription) { throw new UnsupportedOperationException(); }

            @Override
            public Optional<Subscription> findById(String id) { return Optional.empty(); }

            @Override
            public List<Subscription> findAll() { return List.of(); }

            @Override
            public void deleteById(String id) { throw new UnsupportedOperationException(); }

            @Override
            public String getSavedContent(String subscriptionId) { return ""; }
        };
    }

    private RuleGroupRepository emptyRuleGroupRepo() {
        return new RuleGroupRepository() {
            @Override
            public RuleGroup save(RuleGroup ruleGroup) { throw new UnsupportedOperationException(); }

            @Override
            public Optional<RuleGroup> findById(String id) { return Optional.empty(); }

            @Override
            public Optional<RuleGroup> findBySourceSubscriptionId(String subscriptionId) { return Optional.empty(); }

            @Override
            public List<RuleGroup> findAll() { return List.of(); }

            @Override
            public void deleteById(String id) { throw new UnsupportedOperationException(); }
        };
    }

    private ClashConfig configWith(String... nodeNames) {
        ClashConfig c = new ClashConfig("test");
        List<ProxyNode> nodes = new ArrayList<>();
        for (String n : nodeNames) {
            nodes.add(new ProxyNode(n, "ss", "1.1.1.1", 443));
        }
        c.setProxies(nodes);
        return c;
    }

    private ConfigGeneratorServiceImpl newService(ClashConfig... configs) {
        return new ConfigGeneratorServiceImpl(
                null,
                subscriptionServiceStub(configs),
                emptyRuleGroupRepo(),
                new NodeMergeProcessor(),
                new ProxyGroupProcessor());
    }

    private SubscriptionRef ref(String subId, NodePolicy policy) {
        SubscriptionRef r = new SubscriptionRef();
        r.setSubscriptionId(subId);
        r.setNodePolicy(policy);
        return r;
    }

    private NodePolicy excludePolicy(String... keywords) {
        NodePolicy p = new NodePolicy();
        p.setMode(NodePolicy.MODE_ALL);
        p.setExcludeKeywords(Arrays.asList(keywords));
        return p;
    }

    @Test
    void generate_defaultPolicy_keepsAllNodes() {
        ConfigGeneratorServiceImpl service = newService(
                configWith("香港 01", "套餐到期：长期有效"));
        ConfigProfile profile = new ConfigProfile();
        profile.setName("p");
        profile.getSubscriptionRefs().add(ref("A", new NodePolicy()));

        String yaml = service.generate(profile);

        assertTrue(yaml.contains("香港 01"));
        assertTrue(yaml.contains("套餐到期"), "默认规则（全部节点、无排除词）不过滤任何节点");
    }

    @Test
    void generate_excludeKeywords_dropsPseudoNodes() {
        ConfigGeneratorServiceImpl service = newService(
                configWith("香港 01", "套餐到期：长期有效"));
        ConfigProfile profile = new ConfigProfile();
        profile.setName("p");
        profile.getSubscriptionRefs().add(ref("A", excludePolicy("到期")));

        String yaml = service.generate(profile);

        assertTrue(yaml.contains("香港 01"));
        assertFalse(yaml.contains("套餐到期"), "排除关键词应过滤掉伪节点，消除重名冲突");
    }

    @Test
    void generate_oldSubscriptionIdsCompat_behavesAsAll() {
        ConfigGeneratorServiceImpl service = newService(
                configWith("香港 01", "套餐到期：长期有效"));
        ConfigProfile profile = new ConfigProfile();
        profile.setName("p");
        profile.getSubscriptionIds().add("A"); // 旧字段，subscriptionRefs 留空

        String yaml = service.generate(profile);

        assertTrue(yaml.contains("香港 01"));
        assertTrue(yaml.contains("套餐到期"), "老数据映射为默认规则，保留全部节点，行为与升级前一致");
    }

    @Test
    void generate_keywordMatchPolicy_filtersToMatched() {
        ConfigGeneratorServiceImpl service = newService(
                configWith("香港 01", "日本 02", "美国 03", "套餐到期"));
        ConfigProfile profile = new ConfigProfile();
        profile.setName("p");
        NodePolicy policy = new NodePolicy();
        policy.setMode(NodePolicy.MODE_KEYWORD);
        policy.setMatchKeywords(Arrays.asList("香港", "日本"));
        profile.getSubscriptionRefs().add(ref("A", policy));

        String yaml = service.generate(profile);

        assertTrue(yaml.contains("香港 01"));
        assertTrue(yaml.contains("日本 02"));
        assertFalse(yaml.contains("美国 03"));
        assertFalse(yaml.contains("套餐到期"));
    }

    @Test
    void configProfile_jacksonRoundTrip_doesNotDuplicateSubscriptionRefs() throws Exception {
        // 防回归：getEffectiveSubscriptionRefs 被 @JsonIgnore，不应被序列化为字段，
        // 否则反序列化时 subscriptionRefs 会翻倍（曾由端到端测试发现）。
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ConfigProfile p = new ConfigProfile();
        p.setName("t");
        SubscriptionRef ref = new SubscriptionRef();
        ref.setSubscriptionId("s1");
        p.getSubscriptionRefs().add(ref);

        String json = mapper.writeValueAsString(p);
        assertFalse(json.contains("effectiveSubscriptionRefs"),
                "getEffectiveSubscriptionRefs 不应出现在序列化结果中");

        ConfigProfile back = mapper.readValue(json, ConfigProfile.class);
        assertEquals(1, back.getSubscriptionRefs().size(), "序列化往返后 subscriptionRefs 不应翻倍");
        assertEquals(1, back.getEffectiveSubscriptionRefs().size());
    }
}
