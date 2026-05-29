package site.kael.clash.subscription.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.subscription.model.Subscription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JsonFileSubscriptionRepository implements SubscriptionRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public JsonFileSubscriptionRepository(@Value("${data.path:data}") String dataPath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.dataPath = dataPath;
        new File(dataPath + "/subscriptions").mkdirs();
    }

    @Override
    public Subscription save(Subscription subscription) {
        File file = getFile(subscription.getId());
        try {
            objectMapper.writeValue(file, subscription);
            return subscription;
        } catch (IOException e) {
            throw new RuntimeException("保存订阅源失败", e);
        }
    }

    @Override
    public Optional<Subscription> findById(String id) {
        File file = getFile(id);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file, Subscription.class));
        } catch (IOException e) {
            throw new RuntimeException("读取订阅源失败", e);
        }
    }

    @Override
    public List<Subscription> findAll() {
        File dir = new File(dataPath + "/subscriptions");
        List<Subscription> list = new ArrayList<>();
        if (dir.exists()) {
            for (File file : dir.listFiles((d, name) -> name.endsWith(".json"))) {
                try {
                    list.add(objectMapper.readValue(file, Subscription.class));
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
        return new File(dataPath + "/subscriptions/" + id + ".json");
    }
}
