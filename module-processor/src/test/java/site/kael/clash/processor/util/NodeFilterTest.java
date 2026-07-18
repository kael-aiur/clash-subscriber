package site.kael.clash.processor.util;

import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ProxyNode;
import site.kael.clash.processor.model.NodePolicy;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeFilterTest {

    private ProxyNode node(String name) {
        return new ProxyNode(name, "ss", "1.1.1.1", 443);
    }

    @Test
    void containsAnyKeyword_caseInsensitive_andEmpty() {
        assertFalse(NodeFilter.containsAnyKeyword("香港 01", List.of()));
        assertFalse(NodeFilter.containsAnyKeyword("香港 01", null));
        assertFalse(NodeFilter.containsAnyKeyword(null, List.of("x")));
        assertTrue(NodeFilter.containsAnyKeyword("香港 01", List.of("香港")));
        assertTrue(NodeFilter.containsAnyKeyword("HK-01", List.of("hk")));
        assertFalse(NodeFilter.containsAnyKeyword("日本 01", List.of("香港")));
    }

    @Test
    void isAccepted_defaultPolicy_acceptsAll() {
        NodePolicy policy = new NodePolicy();
        assertTrue(NodeFilter.isAccepted(node("套餐到期：长期有效"), policy));
        assertTrue(NodeFilter.isAccepted(node("香港 01"), policy));
    }

    @Test
    void isAccepted_allMode_excludesByKeyword() {
        NodePolicy policy = new NodePolicy();
        policy.setMode(NodePolicy.MODE_ALL);
        policy.setExcludeKeywords(List.of("到期", "剩余", "流量"));
        assertFalse(NodeFilter.isAccepted(node("套餐到期：长期有效"), policy));
        assertFalse(NodeFilter.isAccepted(node("剩余流量：100GB"), policy));
        assertTrue(NodeFilter.isAccepted(node("香港 01"), policy));
    }

    @Test
    void isAccepted_keywordMode_matchesAndExcludes() {
        NodePolicy policy = new NodePolicy();
        policy.setMode(NodePolicy.MODE_KEYWORD);
        policy.setMatchKeywords(List.of("香港", "日本"));
        policy.setExcludeKeywords(List.of("到期"));
        assertTrue(NodeFilter.isAccepted(node("香港 01"), policy));
        assertTrue(NodeFilter.isAccepted(node("日本 02"), policy));
        assertFalse(NodeFilter.isAccepted(node("美国 03"), policy));
        assertFalse(NodeFilter.isAccepted(node("香港套餐到期"), policy));
    }

    @Test
    void isAccepted_keywordMode_emptyMatchKeywords_fallsBackToAll() {
        NodePolicy policy = new NodePolicy();
        policy.setMode(NodePolicy.MODE_KEYWORD);
        policy.setMatchKeywords(List.of());
        policy.setExcludeKeywords(List.of("到期"));
        assertTrue(NodeFilter.isAccepted(node("香港 01"), policy));
        assertFalse(NodeFilter.isAccepted(node("套餐到期"), policy));
    }

    @Test
    void isAccepted_caseInsensitive() {
        NodePolicy policy = new NodePolicy();
        policy.setMode(NodePolicy.MODE_KEYWORD);
        policy.setMatchKeywords(List.of("HK"));
        assertTrue(NodeFilter.isAccepted(node("hk-01"), policy));
        assertTrue(NodeFilter.isAccepted(node("HK-02"), policy));
    }

    @Test
    void filter_returnsNewList_andHandlesNull() {
        List<ProxyNode> nodes = Arrays.asList(node("香港 01"), node("套餐到期"), node("日本 02"));
        NodePolicy policy = new NodePolicy();
        policy.setExcludeKeywords(List.of("到期"));
        List<ProxyNode> filtered = NodeFilter.filter(nodes, policy);
        assertEquals(2, filtered.size());
        assertEquals(3, nodes.size());
        assertTrue(NodeFilter.filter(null, policy).isEmpty());
    }
}
