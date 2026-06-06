package site.kael.clash.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Basic Auth 认证拦截器
 * <p>
 * 对配置获取接口（/api/config/{name}）进行 Basic Auth 认证。
 * 仅当配置设置了认证信息时才要求认证，否则直接放行。
 */
@Component
public class BasicAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthInterceptor.class);

    private final ConfigProfileRepository configProfileRepository;

    public BasicAuthInterceptor(ConfigProfileRepository configProfileRepository) {
        this.configProfileRepository = configProfileRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 只拦截 /api/config/{name} 请求（配置获取接口）
        if (!requestURI.matches("/api/config/[^/]+")) {
            return true;
        }

        // 排除管理接口（/api/config/list, /api/config/detail/{id} 等）
        if (requestURI.contains("/list") || requestURI.contains("/detail/")) {
            return true;
        }

        // 从 URI 中提取配置名称
        String name = extractConfigName(requestURI);
        if (name == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的请求路径");
            return false;
        }

        // 查找配置
        ConfigProfile profile = configProfileRepository.findByName(name).orElse(null);
        if (profile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "配置不存在");
            return false;
        }

        // 如果没有设置认证信息，直接放行
        if (profile.getAuthUsername() == null || profile.getAuthUsername().isEmpty()) {
            return true;
        }

        // 验证 Basic Auth
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Config Profile\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "需要认证");
            return false;
        }

        String credentials = new String(Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);
        if (parts.length != 2) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "认证格式错误");
            return false;
        }

        String username = parts[0];
        String password = parts[1];

        if (!username.equals(profile.getAuthUsername()) || !password.equals(profile.getAuthPassword())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "用户名或密码错误");
            return false;
        }

        log.debug("Basic Auth 认证成功: config={}", name);
        return true;
    }

    private String extractConfigName(String requestURI) {
        // /api/config/{name} -> {name}
        String[] parts = requestURI.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        return null;
    }
}
