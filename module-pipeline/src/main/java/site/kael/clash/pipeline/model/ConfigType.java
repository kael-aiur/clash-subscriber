package site.kael.clash.pipeline.model;

/**
 * 配置类型枚举
 */
public enum ConfigType {

    /** 订阅源模式 */
    SUBSCRIPTION("subscription"),

    /** 配置组合模式 */
    CONFIG_PROFILE("config-profile");

    private final String value;

    ConfigType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 从字符串值转换为 ConfigType 枚举
     *
     * @param value 字符串值
     * @return 对应的 ConfigType 枚举
     * @throws IllegalArgumentException 如果值不匹配任何枚举
     */
    public static ConfigType fromValue(String value) {
        for (ConfigType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的配置类型: " + value);
    }
}
