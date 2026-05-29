package site.kael.clash.mihomo.repository;

import site.kael.clash.mihomo.model.MihomoInstance;

import java.util.List;
import java.util.Optional;

public interface MihomoInstanceRepository {
    MihomoInstance save(MihomoInstance instance);
    Optional<MihomoInstance> findById(String id);
    List<MihomoInstance> findAll();
    void deleteById(String id);
}
