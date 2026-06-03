package site.kael.clash.web.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.web.model.NodeTag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JsonFileNodeTagRepository implements NodeTagRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public JsonFileNodeTagRepository(@Value("${data.path:data}") String dataPath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.dataPath = dataPath;
        new File(dataPath + "/node-tags").mkdirs();
    }

    @Override
    public NodeTag save(NodeTag nodeTag) {
        File file = getFile(nodeTag.getId());
        try {
            objectMapper.writeValue(file, nodeTag);
            return nodeTag;
        } catch (IOException e) {
            throw new RuntimeException("保存节点标签失败", e);
        }
    }

    @Override
    public Optional<NodeTag> findById(String id) {
        File file = getFile(id);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file, NodeTag.class));
        } catch (IOException e) {
            throw new RuntimeException("读取节点标签失败", e);
        }
    }

    @Override
    public List<NodeTag> findAll() {
        File dir = new File(dataPath + "/node-tags");
        List<NodeTag> list = new ArrayList<>();
        if (dir.exists()) {
            for (File file : dir.listFiles((d, name) -> name.endsWith(".json"))) {
                try {
                    list.add(objectMapper.readValue(file, NodeTag.class));
                } catch (IOException e) {
                    // 跳过损坏的文件
                }
            }
        }
        return list;
    }

    @Override
    public void deleteById(String id) {
        File file = getFile(id);
        if (file.exists()) {
            file.delete();
        }
    }

    private File getFile(String id) {
        return new File(dataPath + "/node-tags/" + id + ".json");
    }
}
