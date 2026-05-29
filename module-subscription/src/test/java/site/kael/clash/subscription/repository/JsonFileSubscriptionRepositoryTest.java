package site.kael.clash.subscription.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.kael.clash.subscription.model.Subscription;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileSubscriptionRepositoryTest {

    private JsonFileSubscriptionRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        repository = new JsonFileSubscriptionRepository(tempDir.toString());
    }

    @Test
    void testSaveAndFindById() {
        Subscription sub = new Subscription();
        sub.setId("test-001");
        sub.setName("测试订阅");
        sub.setUrl("https://example.com/sub");
        repository.save(sub);

        Optional<Subscription> found = repository.findById("test-001");
        assertTrue(found.isPresent());
        assertEquals("测试订阅", found.get().getName());
    }

    @Test
    void testFindAll() {
        Subscription sub1 = new Subscription();
        sub1.setId("001");
        sub1.setName("sub1");
        sub1.setUrl("https://example.com/1");
        repository.save(sub1);

        Subscription sub2 = new Subscription();
        sub2.setId("002");
        sub2.setName("sub2");
        sub2.setUrl("https://example.com/2");
        repository.save(sub2);

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void testDeleteById() {
        Subscription sub = new Subscription();
        sub.setId("to-delete");
        sub.setName("delete me");
        sub.setUrl("https://example.com/del");
        repository.save(sub);
        repository.deleteById("to-delete");
        assertFalse(repository.findById("to-delete").isPresent());
    }
}
