package site.kael.clash.web.auth.security;

import org.springframework.stereotype.Component;
import site.kael.clash.web.auth.model.PasswordHash;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordHash hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH, ALGORITHM);
        return new PasswordHash(
                ALGORITHM,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash),
                ITERATIONS,
                KEY_LENGTH
        );
    }

    public boolean matches(String password, PasswordHash stored) {
        byte[] salt = Base64.getDecoder().decode(stored.getSalt());
        byte[] expected = Base64.getDecoder().decode(stored.getHash());
        byte[] actual = pbkdf2(password, salt, stored.getIterations(), stored.getKeyLength(), stored.getAlgorithm());
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLength, String algorithm) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        }
    }
}
