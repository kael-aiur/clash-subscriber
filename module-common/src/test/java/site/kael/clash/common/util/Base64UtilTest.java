package site.kael.clash.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Base64UtilTest {

    @Test
    void testDecode() {
        String encoded = java.util.Base64.getEncoder().encodeToString("hello".getBytes());
        assertEquals("hello", Base64Util.decode(encoded));
    }
}
