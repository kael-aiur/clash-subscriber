package site.kael.clash.web.auth.repository;

import site.kael.clash.web.auth.model.AdminAccount;

import java.util.Optional;

public interface AdminAccountRepository {
    Optional<AdminAccount> find();

    boolean exists();

    AdminAccount save(AdminAccount account);
}
