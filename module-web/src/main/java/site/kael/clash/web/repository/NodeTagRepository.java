package site.kael.clash.web.repository;

import site.kael.clash.web.model.NodeTag;

import java.util.List;
import java.util.Optional;

public interface NodeTagRepository {
    NodeTag save(NodeTag nodeTag);
    Optional<NodeTag> findById(String id);
    List<NodeTag> findAll();
    void deleteById(String id);
}
