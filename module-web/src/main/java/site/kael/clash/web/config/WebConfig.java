package site.kael.clash.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import site.kael.clash.web.auth.BasicAuthInterceptor;
import site.kael.clash.web.auth.interceptor.AuthInterceptor;

/**
 * Web MVC 配置
 * <p>
 * 配置 CORS 策略，允许所有来源访问 API。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final BasicAuthInterceptor basicAuthInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, BasicAuthInterceptor basicAuthInterceptor) {
        this.authInterceptor = authInterceptor;
        this.basicAuthInterceptor = basicAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/config/*");
        registry.addInterceptor(basicAuthInterceptor)
                .addPathPatterns("/api/config/*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 处理 Chrome DevTools 的 well-known 请求
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/.well-known/");
    }
}
