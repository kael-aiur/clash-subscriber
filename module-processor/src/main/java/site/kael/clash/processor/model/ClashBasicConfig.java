package site.kael.clash.processor.model;

/**
 * Clash 基础配置：端口、模式、日志等全局设置。
 */
public class ClashBasicConfig {

    private int mixedPort = 7890;
    private int port = 7891;
    private int socksPort = 7892;
    private int redirPort = 7893;
    private boolean allowLan = false;
    /** 运行模式：rule, global, direct */
    private String mode = "rule";
    private String logLevel = "info";
    private String externalController = "127.0.0.1:9090";
    private String secret = "";

    public ClashBasicConfig() {}

    public int getMixedPort() { return mixedPort; }
    public void setMixedPort(int mixedPort) { this.mixedPort = mixedPort; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getSocksPort() { return socksPort; }
    public void setSocksPort(int socksPort) { this.socksPort = socksPort; }
    public int getRedirPort() { return redirPort; }
    public void setRedirPort(int redirPort) { this.redirPort = redirPort; }
    public boolean isAllowLan() { return allowLan; }
    public void setAllowLan(boolean allowLan) { this.allowLan = allowLan; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    public String getExternalController() { return externalController; }
    public void setExternalController(String externalController) { this.externalController = externalController; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
