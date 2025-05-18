package entities;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Group implements Serializable {
    private String groupName;
    private Set<String> members; // Logins dos membros
    private Set<String> pendingInvites; // Quem está aguardando convite

    public Group(String groupName) {
        this.groupName = groupName;
        this.members = new HashSet<>();
        this.pendingInvites = new HashSet<>();
    }

    public String getGroupName() { return groupName; }
    public Set<String> getMembers() { return members; }
    public Set<String> getPendingInvites() { return pendingInvites; }

    public void addMember(String login) { members.add(login); }
    public void removeMember(String login) { members.remove(login); }
    public boolean isMember(String login) { return members.contains(login); }
    public void invite(String login) { pendingInvites.add(login); }
    public void acceptInvite(String login) {
        pendingInvites.remove(login);
        members.add(login);
    }
    public void declineInvite(String login) {
        pendingInvites.remove(login);
    }
}
