package site.kael.clash.mihomo.model;

import java.util.List;
import java.util.Map;

/**
 * 转发路径查询结果，包含 Vue Flow 格式的节点和边
 */
public class ForwardingPathResult {

    private List<Node> nodes;
    private List<Edge> edges;

    public ForwardingPathResult(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<Node> getNodes() { return nodes; }
    public void setNodes(List<Node> nodes) { this.nodes = nodes; }
    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) { this.edges = edges; }

    /**
     * Vue Flow 节点
     */
    public static class Node {
        private String id;
        private String type;  // domain, rule, proxyGroup, proxy, target
        private Map<String, Object> data;
        private Position position;

        public Node(String id, String type, Map<String, Object> data) {
            this.id = id;
            this.type = type;
            this.data = data;
            this.position = new Position(0, 0);  // 布局由前端 dagre 计算
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        public Position getPosition() { return position; }
        public void setPosition(Position position) { this.position = position; }
    }

    /**
     * Vue Flow 边
     */
    public static class Edge {
        private String id;
        private String source;
        private String target;

        public Edge(String id, String source, String target) {
            this.id = id;
            this.source = source;
            this.target = target;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }

    /**
     * 节点位置（前端 dagre 会重新计算）
     */
    public static class Position {
        private double x;
        private double y;

        public Position(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
    }
}
