package site.kael.clash.web.auth.dto;

public class AuthStatusResponse {
    private boolean initialized;
    private boolean authenticated;
    private String username;

    public AuthStatusResponse() {
    }

    public AuthStatusResponse(boolean initialized, boolean authenticated, String username) {
        this.initialized = initialized;
        this.authenticated = authenticated;
        this.username = username;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
