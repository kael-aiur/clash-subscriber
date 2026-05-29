package site.kael.clash.common.util;

import java.util.Base64;

public class Base64Util {
    public static String decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded));
    }

    public static String encode(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }
}
