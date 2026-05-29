# 模块设计实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 clash-subscriber 项目的 Maven 多模块结构和核心基础设施

**Architecture:** 领域驱动模块架构，6 个模块（common、subscription、processor、mihomo、scheduler、web），通过接口通信，JSON 文件存储

**Tech Stack:** Java 21, Spring Boot 3, Maven, GraalVM JS, Jackson, SnakeYAML, OkHttp

---

## Task 1: 创建 Maven 多模块项目结构

**Files:**
- Modify: `pom.xml`（父 POM）
- Create: `module-common/pom.xml`
- Create: `module-subscription/pom.xml`
- Create: `module-processor/pom.xml`
- Create: `module-mihomo/pom.xml`
- Create: `module-scheduler/pom.xml`
- Create: `module-web/pom.xml`

- [ ] **Step 1: 创建父 POM（模块聚合）**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>site.kael.clash</groupId>
    <artifactId>clash-subscriber</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>module-common</module>
        <module>module-subscription</module>
        <module>module-processor</module>
        <module>module-mihomo</module>
        <module>module-scheduler</module>
        <module>module-web</module>
    </modules>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.5</spring-boot.version>
        <graalvm.version>23.1.2</graalvm.version>
        <okhttp.version>4.12.0</okhttp.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.graalvm.polyglot</groupId>
                <artifactId>polyglot</artifactId>
                <version>${graalvm.version}</version>
            </dependency>
            <dependency>
                <groupId>com.squareup.okhttp3</groupId>
                <artifactId>okhttp</artifactId>
                <version>${okhttp.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 module-common/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>site.kael.clash</groupId>
        <artifactId>clash-subscriber</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>module-common</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.yaml</groupId>
            <artifactId>snakeyaml</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 创建 module-subscription/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>site.kael.clash</groupId>
        <artifactId>clash-subscriber</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>module-subscription</artifactId>

    <dependencies>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: 创建 module-processor/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>site.kael.clash</groupId>
        <artifactId>clash-subscriber</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>module-processor</artifactId>

    <dependencies>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-subscription</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.graalvm.polyglot</groupId>
            <artifactId>polyglot</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: 创建 module-mihomo/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>site.kael.clash</groupId>
        <artifactId>clash-subscriber</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>module-mihomo</artifactId>

    <dependencies>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 6: 创建 module-scheduler/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>site.kael.clash</groupId>
        <artifactId>clash-subscriber</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>module-scheduler</artifactId>

    <dependencies>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-subscription</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-processor</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-mihomo</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 7: 创建 module-web/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>site.kael.clash</groupId>
        <artifactId>clash-subscriber</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>module-web</artifactId>

    <dependencies>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-subscription</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-processor</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-mihomo</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>site.kael.clash</groupId>
            <artifactId>module-scheduler</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 8: 验证 Maven 项目结构**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add pom.xml module-common/pom.xml module-subscription/pom.xml module-processor/pom.xml module-mihomo/pom.xml module-scheduler/pom.xml module-web/pom.xml
git commit -m "chore: 创建 Maven 多模块项目结构"
```

---

## Task 2: 创建 data 目录结构

**Files:**
- Create: `data/subscriptions/.gitkeep`
- Create: `data/mihomo-instances/.gitkeep`
- Create: `data/scripts/.gitkeep`
- Create: `data/cache/.gitkeep`
- Modify: `.gitignore`

- [ ] **Step 1: 创建 data 目录和 .gitkeep 文件**

```bash
mkdir -p data/subscriptions data/mihomo-instances data/scripts data/cache
touch data/subscriptions/.gitkeep data/mihomo-instances/.gitkeep data/scripts/.gitkeep data/cache/.gitkeep
```

- [ ] **Step 2: 更新 .gitignore 添加 data 目录规则**

在 `.gitignore` 末尾添加：

```
# Data directory (keep structure, ignore content)
data/**/*
!data/**/.gitkeep
```

- [ ] **Step 3: 验证目录结构**

Run: `find data -type f`
Expected: 4 个 .gitkeep 文件

- [ ] **Step 4: 提交**

```bash
git add data/ .gitignore
git commit -m "chore: 创建 data 目录结构"
```

---

## Task 3: 创建 module-common 共享模型

**Files:**
- Create: `module-common/src/main/java/site/kael/clash/common/model/ProxyNode.java`
- Create: `module-common/src/main/java/site/kael/clash/common/model/ClashConfig.java`
- Create: `module-common/src/test/java/site/kael/clash/common/model/ProxyNodeTest.java`
- Create: `module-common/src/test/java/site/kael/clash/common/model/ClashConfigTest.java`

- [ ] **Step 1: 创建 ProxyNode 模型**

```java
package site.kael.clash.common.model;

import java.util.HashMap;
import java.util.Map;

public class ProxyNode {
    private String name;
    private String type;
    private String server;
    private int port;
    private Map<String, Object> extra = new HashMap<>();

    public ProxyNode() {}

    public ProxyNode(String name, String type, String server, int port) {
        this.name = name;
        this.type = type;
        this.server = server;
        this.port = port;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
```

- [ ] **Step 2: 创建 ProxyNode 测试**

```java
package site.kael.clash.common.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProxyNodeTest {

    @Test
    void testCreateProxyNode() {
        ProxyNode node = new ProxyNode("node1", "vmess", "1.2.3.4", 443);
        assertEquals("node1", node.getName());
        assertEquals("vmess", node.getType());
        assertEquals("1.2.3.4", node.getServer());
        assertEquals(443, node.getPort());
    }

    @Test
    void testExtraMap() {
        ProxyNode node = new ProxyNode();
        node.getExtra().put("uuid", "test-uuid");
        assertEquals("test-uuid", node.getExtra().get("uuid"));
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `mvn test -pl module-common`
Expected: Tests passed

- [ ] **Step 4: 创建 ClashConfig 模型**

```java
package site.kael.clash.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClashConfig {
    private String name;
    private Map<String, Object> raw = new HashMap<>();
    private List<ProxyNode> proxies = new ArrayList<>();
    private Map<String, Object> proxyGroups = new HashMap<>();
    private List<Object> rules = new ArrayList<>();

    public ClashConfig() {}

    public ClashConfig(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Object> getRaw() { return raw; }
    public void setRaw(Map<String, Object> raw) { this.raw = raw; }
    public List<ProxyNode> getProxies() { return proxies; }
    public void setProxies(List<ProxyNode> proxies) { this.proxies = proxies; }
    public Map<String, Object> getProxyGroups() { return proxyGroups; }
    public void setProxyGroups(Map<String, Object> proxyGroups) { this.proxyGroups = proxyGroups; }
    public List<Object> getRules() { return rules; }
    public void setRules(List<Object> rules) { this.rules = rules; }
}
```

- [ ] **Step 5: 创建 ClashConfig 测试**

```java
package site.kael.clash.common.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClashConfigTest {

    @Test
    void testCreateClashConfig() {
        ClashConfig config = new ClashConfig("test-config");
        assertEquals("test-config", config.getName());
        assertTrue(config.getProxies().isEmpty());
        assertTrue(config.getRules().isEmpty());
    }

    @Test
    void testAddProxy() {
        ClashConfig config = new ClashConfig();
        config.getProxies().add(new ProxyNode("node1", "vmess", "1.2.3.4", 443));
        assertEquals(1, config.getProxies().size());
    }
}
```

- [ ] **Step 6: 运行测试**

Run: `mvn test -pl module-common`
Expected: Tests passed

- [ ] **Step 7: 提交**

```bash
git add module-common/src/
git commit -m "feat(common): 创建 ProxyNode 和 ClashConfig 模型"
```

---

## Task 4: 创建异常定义和工具类

**Files:**
- Create: `module-common/src/main/java/site/kael/clash/common/exception/BusinessException.java`
- Create: `module-common/src/main/java/site/kael/clash/common/util/YamlUtil.java`
- Create: `module-common/src/main/java/site/kael/clash/common/util/Base64Util.java`
- Create: `module-common/src/main/java/site/kael/clash/common/util/IdGenerator.java`
- Create: `module-common/src/test/java/site/kael/clash/common/util/YamlUtilTest.java`
- Create: `module-common/src/test/java/site/kael/clash/common/util/Base64UtilTest.java`

- [ ] **Step 1: 创建 BusinessException**

```java
package site.kael.clash.common.exception;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
```

- [ ] **Step 2: 创建 YamlUtil**

```java
package site.kael.clash.common.util;

import org.yaml.snakeyaml.Yaml;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.common.model.ProxyNode;

import java.util.List;
import java.util.Map;

public class YamlUtil {
    private static final Yaml yaml = new Yaml();

    public static Map<String, Object> parseYaml(String content) {
        return yaml.load(content);
    }

    public static ClashConfig parseClashConfig(String content) {
        Map<String, Object> data = yaml.load(content);
        ClashConfig config = new ClashConfig();
        config.setRaw(data);

        if (data.containsKey("proxies")) {
            List<Map<String, Object>> proxies = (List<Map<String, Object>>) data.get("proxies");
            for (Map<String, Object> proxy : proxies) {
                ProxyNode node = new ProxyNode();
                node.setName((String) proxy.get("name"));
                node.setType((String) proxy.get("type"));
                node.setServer((String) proxy.get("server"));
                node.setPort((Integer) proxy.get("port"));
                proxy.remove("name");
                proxy.remove("type");
                proxy.remove("server");
                proxy.remove("port");
                node.setExtra(proxy);
                config.getProxies().add(node);
            }
        }

        return config;
    }
}
```

- [ ] **Step 3: 创建 YamlUtil 测试**

```java
package site.kael.clash.common.util;

import org.junit.jupiter.api.Test;
import site.kael.clash.common.model.ClashConfig;
import static org.junit.jupiter.api.Assertions.*;

class YamlUtilTest {

    @Test
    void testParseClashConfig() {
        String yaml = """
                proxies:
                  - name: node1
                    type: vmess
                    server: 1.2.3.4
                    port: 443
                """;
        ClashConfig config = YamlUtil.parseClashConfig(yaml);
        assertEquals(1, config.getProxies().size());
        assertEquals("node1", config.getProxies().get(0).getName());
    }
}
```

- [ ] **Step 4: 创建 Base64Util**

```java
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
```

- [ ] **Step 5: 创建 Base64Util 测试**

```java
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
```

- [ ] **Step 6: 创建 IdGenerator**

```java
package site.kael.clash.common.util;

import java.util.UUID;

public class IdGenerator {
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
```

- [ ] **Step 7: 运行测试**

Run: `mvn test -pl module-common`
Expected: Tests passed

- [ ] **Step 8: 提交**

```bash
git add module-common/src/
git commit -m "feat(common): 创建异常定义和工具类"
```

---

## Task 5: 创建 Subscription 模型和 Repository 接口

**Files:**
- Create: `module-subscription/src/main/java/site/kael/clash/subscription/model/Subscription.java`
- Create: `module-subscription/src/main/java/site/kael/clash/subscription/repository/SubscriptionRepository.java`

- [ ] **Step 1: 创建 Subscription 模型**

```java
package site.kael.clash.subscription.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Subscription {
    private String id;
    private String name;
    private String url;
    private String userAgent;
    private Map<String, String> headers = new HashMap<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastFetchedAt;

    public Subscription() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
}
```

- [ ] **Step 2: 创建 SubscriptionRepository 接口**

```java
package site.kael.clash.subscription.repository;

import site.kael.clash.subscription.model.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(String id);
    List<Subscription> findAll();
    void deleteById(String id);
}
```

- [ ] **Step 3: 提交**

```bash
git add module-subscription/src/
git commit -m "feat(subscription): 创建 Subscription 模型和 Repository 接口"
```

---

## Task 6: 实现 JsonFileSubscriptionRepository

**Files:**
- Create: `module-subscription/src/main/java/site/kael/clash/subscription/repository/JsonFileSubscriptionRepository.java`
- Create: `module-subscription/src/test/java/site/kael/clash/subscription/repository/JsonFileSubscriptionRepositoryTest.java`

- [ ] **Step 1: 创建 JsonFileSubscriptionRepository**

```java
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
                    // skip corrupted file
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
```

- [ ] **Step 2: 创建测试**

```java
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
```

- [ ] **Step 3: 运行测试**

Run: `mvn test -pl module-subscription`
Expected: Tests passed

- [ ] **Step 4: 提交**

```bash
git add module-subscription/src/
git commit -m "feat(subscription): 实现 JsonFileSubscriptionRepository"
```

---

## Task 7: 创建 MihomoInstance 模型和 Repository

**Files:**
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/model/HealthStatus.java`
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/model/MihomoInstance.java`
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/repository/MihomoInstanceRepository.java`
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/repository/JsonFileMihomoInstanceRepository.java`
- Create: `module-mihomo/src/test/java/site/kael/clash/mihomo/repository/JsonFileMihomoInstanceRepositoryTest.java`

- [ ] **Step 1: 创建 HealthStatus 枚举**

```java
package site.kael.clash.mihomo.model;

public enum HealthStatus {
    HEALTHY, UNHEALTHY, UNKNOWN
}
```

- [ ] **Step 2: 创建 MihomoInstance 模型**

```java
package site.kael.clash.mihomo.model;

import java.time.LocalDateTime;

public class MihomoInstance {
    private String id;
    private String name;
    private String apiUrl;
    private String apiSecret;
    private boolean enabled = true;
    private HealthStatus status = HealthStatus.UNKNOWN;
    private LocalDateTime lastHealthCheck;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public HealthStatus getStatus() { return status; }
    public void setStatus(HealthStatus status) { this.status = status; }
    public LocalDateTime getLastHealthCheck() { return lastHealthCheck; }
    public void setLastHealthCheck(LocalDateTime lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
}
```

- [ ] **Step 3: 创建 MihomoInstanceRepository 接口**

```java
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
```

- [ ] **Step 4: 实现 JsonFileMihomoInstanceRepository**

```java
package site.kael.clash.mihomo.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.mihomo.model.MihomoInstance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JsonFileMihomoInstanceRepository implements MihomoInstanceRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public JsonFileMihomoInstanceRepository(@Value("${data.path:data}") String dataPath) {
        this.objectMapper = new ObjectMapper();
        this.dataPath = dataPath;
        new File(dataPath + "/mihomo-instances").mkdirs();
    }

    @Override
    public MihomoInstance save(MihomoInstance instance) {
        File file = getFile(instance.getId());
        try {
            objectMapper.writeValue(file, instance);
            return instance;
        } catch (IOException e) {
            throw new RuntimeException("保存 Mihomo 实例失败", e);
        }
    }

    @Override
    public Optional<MihomoInstance> findById(String id) {
        File file = getFile(id);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file, MihomoInstance.class));
        } catch (IOException e) {
            throw new RuntimeException("读取 Mihomo 实例失败", e);
        }
    }

    @Override
    public List<MihomoInstance> findAll() {
        File dir = new File(dataPath + "/mihomo-instances");
        List<MihomoInstance> list = new ArrayList<>();
        if (dir.exists()) {
            for (File file : dir.listFiles((d, name) -> name.endsWith(".json"))) {
                try {
                    list.add(objectMapper.readValue(file, MihomoInstance.class));
                } catch (IOException e) {
                    // skip corrupted file
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
        return new File(dataPath + "/mihomo-instances/" + id + ".json");
    }
}
```

- [ ] **Step 5: 创建测试**

```java
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
```

- [ ] **Step 6: 运行测试**

Run: `mvn test -pl module-mihomo`
Expected: Tests passed

- [ ] **Step 7: 提交**

```bash
git add module-mihomo/src/
git commit -m "feat(mihomo): 创建 MihomoInstance 模型和 Repository"
```

---

## Task 8: 创建 ConfigProcessor 接口和 ProcessingContext

**Files:**
- Create: `module-processor/src/main/java/site/kael/clash/processor/api/ConfigProcessor.java`
- Create: `module-processor/src/main/java/site/kael/clash/processor/api/ProcessingContext.java`

- [ ] **Step 1: 创建 ProcessingContext**

```java
package site.kael.clash.processor.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessingContext {
    private Map<String, Object> variables = new HashMap<>();
    private List<String> logs = new ArrayList<>();

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public void addLog(String message) {
        logs.add(message);
    }

    public List<String> getLogs() {
        return logs;
    }
}
```

- [ ] **Step 2: 创建 ConfigProcessor 接口**

```java
package site.kael.clash.processor.api;

import site.kael.clash.common.model.ClashConfig;

public interface ConfigProcessor {
    String getName();
    int getOrder();
    ClashConfig process(ClashConfig input, ProcessingContext context);
}
```

- [ ] **Step 3: 提交**

```bash
git add module-processor/src/
git commit -m "feat(processor): 创建 ConfigProcessor 接口和 ProcessingContext"
```

---

## Task 9: 创建 Pipeline 配置模型

**Files:**
- Create: `module-processor/src/main/java/site/kael/clash/processor/model/PipelineStep.java`
- Create: `module-processor/src/main/java/site/kael/clash/processor/model/PipelineConfig.java`

- [ ] **Step 1: 创建 PipelineStep**

```java
package site.kael.clash.processor.model;

import java.util.HashMap;
import java.util.Map;

public class PipelineStep {
    private String processor;
    private Map<String, Object> config = new HashMap<>();

    public String getProcessor() { return processor; }
    public void setProcessor(String processor) { this.processor = processor; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
}
```

- [ ] **Step 2: 创建 PipelineConfig**

```java
package site.kael.clash.processor.model;

import java.util.ArrayList;
import java.util.List;

public class PipelineConfig {
    private String id;
    private String name;
    private List<PipelineStep> steps = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<PipelineStep> getSteps() { return steps; }
    public void setSteps(List<PipelineStep> steps) { this.steps = steps; }
}
```

- [ ] **Step 3: 提交**

```bash
git add module-processor/src/
git commit -m "feat(processor): 创建 Pipeline 配置模型"
```

---

## Task 10: 创建 MihomoHttpClient

**Files:**
- Create: `module-mihomo/src/main/java/site/kael/clash/mihomo/client/MihomoHttpClient.java`
- Create: `module-mihomo/src/test/java/site/kael/clash/mihomo/client/MihomoHttpClientTest.java`

- [ ] **Step 1: 创建 MihomoHttpClient**

```java
package site.kael.clash.mihomo.client;

import okhttp3.*;
import org.springframework.stereotype.Component;
import site.kael.clash.common.exception.BusinessException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class MihomoHttpClient {

    private final OkHttpClient client;

    public MihomoHttpClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public void pushConfig(String apiUrl, String apiSecret, String yamlConfig) {
        Request.Builder builder = new Request.Builder()
                .url(apiUrl + "/configs")
                .put(RequestBody.create(yamlConfig, MediaType.parse("application/x-yaml")));

        if (apiSecret != null && !apiSecret.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiSecret);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new BusinessException("推送配置失败: HTTP " + response.code());
            }
        } catch (IOException e) {
            throw new BusinessException("推送配置失败: " + e.getMessage());
        }
    }

    public boolean checkHealth(String apiUrl, String apiSecret) {
        Request.Builder builder = new Request.Builder()
                .url(apiUrl + "/version")
                .get();

        if (apiSecret != null && !apiSecret.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiSecret);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add module-mihomo/src/
git commit -m "feat(mihomo): 创建 MihomoHttpClient"
```

---

## Task 11: 创建 ScheduledTask 模型

**Files:**
- Create: `module-scheduler/src/main/java/site/kael/clash/scheduler/model/TaskStatus.java`
- Create: `module-scheduler/src/main/java/site/kael/clash/scheduler/model/ScheduledTask.java`

- [ ] **Step 1: 创建 TaskStatus 枚举**

```java
package site.kael.clash.scheduler.model;

public enum TaskStatus {
    SUCCESS, FAILED, RUNNING
}
```

- [ ] **Step 2: 创建 ScheduledTask 模型**

```java
package site.kael.clash.scheduler.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduledTask {
    private String id;
    private String name;
    private String pipelineId;
    private List<String> targetInstances = new ArrayList<>();
    private String cronExpression;
    private boolean enabled = true;
    private LocalDateTime lastRunAt;
    private TaskStatus lastRunStatus;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPipelineId() { return pipelineId; }
    public void setPipelineId(String pipelineId) { this.pipelineId = pipelineId; }
    public List<String> getTargetInstances() { return targetInstances; }
    public void setTargetInstances(List<String> targetInstances) { this.targetInstances = targetInstances; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public TaskStatus getLastRunStatus() { return lastRunStatus; }
    public void setLastRunStatus(TaskStatus lastRunStatus) { this.lastRunStatus = lastRunStatus; }
}
```

- [ ] **Step 3: 提交**

```bash
git add module-scheduler/src/
git commit -m "feat(scheduler): 创建 ScheduledTask 模型"
```

---

## Task 12: 创建 Spring Boot Application 类

**Files:**
- Create: `module-web/src/main/java/site/kael/clash/web/ClashSubscriberApplication.java`
- Create: `module-web/src/main/resources/application.yml`

- [ ] **Step 1: 创建 Application 类**

```java
package site.kael.clash.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "site.kael.clash")
public class ClashSubscriberApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClashSubscriberApplication.class, args);
    }
}
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

data:
  path: data

spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
```

- [ ] **Step 3: 验证项目编译**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add module-web/src/
git commit -m "feat(web): 创建 Spring Boot Application 类和配置"
```

---

## Task 13: 最终验证

- [ ] **Step 1: 运行全量测试**

Run: `mvn test`
Expected: All tests passed

- [ ] **Step 2: 打包验证**

Run: `mvn package -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "chore: 模块设计实现完成"
```
