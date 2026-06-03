package site.kael.clash.web.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.common.util.IdGenerator;
import site.kael.clash.web.model.NodeTag;
import site.kael.clash.web.repository.NodeTagRepository;
import site.kael.clash.web.service.NodeTagService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class NodeTagServiceImpl implements NodeTagService {

    private static final Logger log = LoggerFactory.getLogger(NodeTagServiceImpl.class);

    private final NodeTagRepository repository;

    public NodeTagServiceImpl(NodeTagRepository repository) {
        this.repository = repository;
    }

    @Override
    public NodeTag create(NodeTag nodeTag) {
        nodeTag.setId(IdGenerator.generate());
        nodeTag.setCreatedAt(LocalDateTime.now());
        nodeTag.setUpdatedAt(LocalDateTime.now());
        log.info("创建节点标签: name={}, priority={}", nodeTag.getName(), nodeTag.getPriority());
        return repository.save(nodeTag);
    }

    @Override
    public NodeTag update(NodeTag nodeTag) {
        NodeTag existing = repository.findById(nodeTag.getId())
                .orElseThrow(() -> new BusinessException(404, "节点标签不存在: " + nodeTag.getId()));
        existing.setName(nodeTag.getName());
        existing.setPriority(nodeTag.getPriority());
        existing.setPatterns(nodeTag.getPatterns());
        existing.setUpdatedAt(LocalDateTime.now());
        log.info("更新节点标签: id={}", nodeTag.getId());
        return repository.save(existing);
    }

    @Override
    public List<NodeTag> findAll() {
        List<NodeTag> list = repository.findAll();
        list.sort(Comparator.comparingInt(NodeTag::getPriority));
        return list;
    }

    @Override
    public NodeTag findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "节点标签不存在: " + id));
    }

    @Override
    public void deleteById(String id) {
        log.info("删除节点标签: id={}", id);
        repository.deleteById(id);
    }
}
