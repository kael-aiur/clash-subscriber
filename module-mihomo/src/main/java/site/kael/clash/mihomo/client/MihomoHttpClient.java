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
