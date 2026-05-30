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
        String jsonBody = "{\"payload\":" + toJsonString(yamlConfig) + "}";
        Request.Builder builder = new Request.Builder()
                .url(apiUrl + "/configs")
                .put(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        if (apiSecret != null && !apiSecret.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiSecret);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new BusinessException("推送配置失败: HTTP " + response.code() + " - " + body);
            }
        } catch (IOException e) {
            throw new BusinessException("推送配置失败: " + e.getMessage());
        }
    }

    /**
     * 从 Mihomo 实例获取当前运行配置
     *
     * @param apiUrl    Mihomo API 地址
     * @param apiSecret API 密钥（可为 null）
     * @return YAML 格式的配置字符串
     */
    public String getConfig(String apiUrl, String apiSecret) {
        Request.Builder builder = new Request.Builder()
                .url(apiUrl + "/configs")
                .get();

        if (apiSecret != null && !apiSecret.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiSecret);
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new BusinessException("获取配置失败: HTTP " + response.code() + " - " + body);
            }
            return response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            throw new BusinessException("获取配置失败: " + e.getMessage());
        }
    }

    private static String toJsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
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
