package site.kael.clash.common.util;

import java.util.UUID;

public class IdGenerator {
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
