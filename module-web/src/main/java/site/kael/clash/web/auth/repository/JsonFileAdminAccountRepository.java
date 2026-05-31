package site.kael.clash.web.auth.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.web.auth.model.AdminAccount;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Repository
public class JsonFileAdminAccountRepository implements AdminAccountRepository {
    private final ObjectMapper objectMapper;
    private final File accountFile;

    public JsonFileAdminAccountRepository(@Value("${data.path:data}") String dataPath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        File dir = new File(dataPath + "/admin");
        dir.mkdirs();
        this.accountFile = new File(dir, "admin.json");
    }

    @Override
    public Optional<AdminAccount> find() {
        if (!accountFile.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(accountFile, AdminAccount.class));
        } catch (IOException e) {
            throw new AdminAccountReadException("管理员账号文件无法读取", e);
        }
    }

    @Override
    public boolean exists() {
        return accountFile.exists();
    }

    @Override
    public AdminAccount save(AdminAccount account) {
        try {
            objectMapper.writeValue(accountFile, account);
            return account;
        } catch (IOException e) {
            throw new RuntimeException("保存管理员账号失败", e);
        }
    }
}
