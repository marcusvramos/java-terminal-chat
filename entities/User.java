package entities;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {
    private String name;
    private String login;
    private String email;
    private String passwordHash;
    private String status;
    private Set<String> allowedPrivateChats; // Quem já foi aceito para conversar

    public User(String name, String login, String email, String passwordHash, String status) {
        this.name = name;
        this.login = login;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.allowedPrivateChats = new HashSet<>();
    }

    public String getName() { return name; }
    public String getLogin() { return login; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Set<String> getAllowedPrivateChats() { return allowedPrivateChats; }
    public void allowPrivateChat(String login) { allowedPrivateChats.add(login); }
    public boolean isAllowed(String login) { return allowedPrivateChats.contains(login); }
}
