package site.kael.clash.mihomo.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.mihomo.model.MihomoInstance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JsonFileMihomoInstanceRepository implements MihomoInstanceRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public JsonFileMihomoInstanceRepository(@Value("${data.path:data}") String dataPath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.dataPath = dataPath;
        new File(dataPath + "/mihomo-instances").mkdirs();
    }

    @Override
    public MihomoInstance save(MihomoInstance instance) {
        File file = getFile(instance.getId());
        try {
            objectMapper.writeValue(file, instance);
            return instance;
        } catch (IOException e) {
            throw new RuntimeException("保存 Mihomo 实例失败", e);
        }
    }

    @Override
    public Optional<MihomoInstance> findById(String id) {
        File file = getFile(id);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file, MihomoInstance.class));
        } catch (IOException e) {
            throw new RuntimeException("读取 Mihomo 实例失败", e);
        }
    }

    @Override
    public List<MihomoInstance> findAll() {
        File dir = new File(dataPath + "/mihomo-instances");
        List<MihomoInstance> list = new ArrayList<>();
        if (dir.exists()) {
            for (File file : dir.listFiles((d, name) -> name.endsWith(".json"))) {
                try {
                    list.add(objectMapper.readValue(file, MihomoInstance.class));
                } catch (IOException e) {
                    // skip corrupted file
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
        return new File(dataPath + "/mihomo-instances/" + id + ".json");
    }
}
