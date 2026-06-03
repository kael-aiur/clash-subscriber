package site.kael.clash.web.service;

import site.kael.clash.web.model.NodeTag;

import java.util.List;

public interface NodeTagService {

    /**
     * 创建节点标签，自动生成 ID 和创建时间。
     */
    NodeTag create(NodeTag nodeTag);

    /**
     * 更新已有节点标签。
     */
    NodeTag update(NodeTag nodeTag);

    /**
     * 查询全部节点标签，按优先级升序排列。
     */
    List<NodeTag> findAll();

    /**
     * 按 ID 查询节点标签。
     */
    NodeTag findById(String id);

    /**
     * 按 ID 删除节点标签。
     */
    void deleteById(String id);
}
