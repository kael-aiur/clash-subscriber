package site.kael.clash.processor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点采纳规则：控制某订阅源的哪些节点进入最终配置。
 * <ul>
 *   <li>mode=all —— 采纳全部节点，排除命中 excludeKeywords 的节点</li>
 *   <li>mode=keyword —— 仅采纳命中 matchKeywords 且未被 excludeKeywords 排除的节点；
 *       matchKeywords 为空时回退为 all 模式</li>
 * </ul>
 */
public class NodePolicy {

    public static final String MODE_ALL = "all";
    public static final String MODE_KEYWORD = "keyword";

    private String mode = MODE_ALL;
    private List<String> excludeKeywords = new ArrayList<>();
    private List<String> matchKeywords = new ArrayList<>();

    public NodePolicy() {
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<String> getExcludeKeywords() {
        return excludeKeywords;
    }

    public void setExcludeKeywords(List<String> excludeKeywords) {
        this.excludeKeywords = excludeKeywords;
    }

    public List<String> getMatchKeywords() {
        return matchKeywords;
    }

    public void setMatchKeywords(List<String> matchKeywords) {
        this.matchKeywords = matchKeywords;
    }
}
