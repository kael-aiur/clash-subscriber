package site.kael.clash.web.auth.model;

public class PasswordHash {
    private String algorithm;
    private String salt;
    private String hash;
    private int iterations;
    private int keyLength;

    public PasswordHash() {
    }

    public PasswordHash(String algorithm, String salt, String hash, int iterations, int keyLength) {
        this.algorithm = algorithm;
        this.salt = salt;
        this.hash = hash;
        this.iterations = iterations;
        this.keyLength = keyLength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }
}
