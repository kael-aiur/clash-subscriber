package site.kael.clash.mihomo.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.kael.clash.mihomo.model.HealthStatus;
import site.kael.clash.mihomo.model.MihomoInstance;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileMihomoInstanceRepositoryTest {

    private JsonFileMihomoInstanceRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        repository = new JsonFileMihomoInstanceRepository(tempDir.toString());
    }

    @Test
    void testSaveAndFindById() {
        MihomoInstance instance = new MihomoInstance();
        instance.setId("inst-001");
        instance.setName("本地 Mihomo");
        instance.setApiUrl("http://localhost:9090");
        repository.save(instance);

        var found = repository.findById("inst-001");
        assertTrue(found.isPresent());
        assertEquals("本地 Mihomo", found.get().getName());
        assertEquals(HealthStatus.UNKNOWN, found.get().getStatus());
    }
}
