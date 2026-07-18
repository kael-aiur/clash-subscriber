package site.kael.clash.processor.model;

/**
 * 配置组合中对一个订阅源的引用，附带该订阅源的节点采纳规则。
 */
public class SubscriptionRef {

    private String subscriptionId;
    private NodePolicy nodePolicy = new NodePolicy();

    public SubscriptionRef() {
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public NodePolicy getNodePolicy() {
        return nodePolicy;
    }

    public void setNodePolicy(NodePolicy nodePolicy) {
        this.nodePolicy = nodePolicy;
    }
}
