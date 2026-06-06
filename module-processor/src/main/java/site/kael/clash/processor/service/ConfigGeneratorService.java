package site.kael.clash.processor.service;

import site.kael.clash.processor.model.ConfigProfile;

/**
 * 配置生成服务：根据配置组合生成完整的 Clash YAML 配置。
 */
public interface ConfigGeneratorService {

    /**
     * 根据配置组合生成 Clash YAML 配置
     *
     * @param profile 配置组合
     * @return Clash YAML 配置字符串
     */
    String generate(ConfigProfile profile);

    /**
     * 根据配置名称生成 Clash YAML 配置
     *
     * @param name 配置名称
     * @return Clash YAML 配置字符串
     */
    String generateByName(String name);
}
