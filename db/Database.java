package db;
import java.sql.*;
import java.util.*;
import entities.*;

public class Database {
    private Connection conn;

    public Database(String dbFile) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        initialize();
    }

    private void initialize() throws SQLException {
        Statement s = conn.createStatement();
        s.execute("CREATE TABLE IF NOT EXISTS users (name TEXT, login TEXT PRIMARY KEY, email TEXT, passwordHash TEXT, status TEXT)");
        s.execute("CREATE TABLE IF NOT EXISTS groups (groupName TEXT PRIMARY KEY)");
        s.execute("CREATE TABLE IF NOT EXISTS group_members (groupName TEXT, login TEXT)");
        s.execute("CREATE TABLE IF NOT EXISTS group_pending (groupName TEXT, login TEXT)");
        s.execute("CREATE TABLE IF NOT EXISTS messages (sender TEXT, recipient TEXT, content TEXT, timestamp TEXT, delivered INTEGER)");
        s.close();
    }

    // Métodos de CRUD (iguais aos anteriores), mas agora para pendingInvites:
    public boolean registerUser(User user) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO users VALUES (?,?,?,?,?)");
        ps.setString(1, user.getName());
        ps.setString(2, user.getLogin());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getPasswordHash());
        ps.setString(5, user.getStatus());
        try { ps.execute(); return true; } catch (SQLException ex) { return false; }
    }

    public User getUserByLogin(String login) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE login=?");
        ps.setString(1, login);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new User(rs.getString("name"), login, rs.getString("email"), rs.getString("passwordHash"), rs.getString("status"));
        }
        return null;
    }

    public void updateUserStatus(String login, String status) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE users SET status=? WHERE login=?");
        ps.setString(1, status);
        ps.setString(2, login);
        ps.execute();
    }

    public List<User> getOnlineUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery("SELECT * FROM users WHERE status='online'");
        while (rs.next()) {
            users.add(new User(rs.getString("name"), rs.getString("login"), rs.getString("email"), rs.getString("passwordHash"), rs.getString("status")));
        }
        return users;
    }

    public boolean createGroup(String groupName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO groups VALUES (?)");
        ps.setString(1, groupName);
        try { ps.execute(); return true; } catch (SQLException ex) { return false; }
    }

    public boolean groupExists(String groupName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM groups WHERE groupName=?");
        ps.setString(1, groupName);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public void addMemberToGroup(String groupName, String login) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO group_members VALUES (?, ?)");
        ps.setString(1, groupName);
        ps.setString(2, login);
        ps.execute();
    }

    public void removeMemberFromGroup(String groupName, String login) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM group_members WHERE groupName=? AND login=?");
        ps.setString(1, groupName);
        ps.setString(2, login);
        ps.execute();
    }

    public List<String> getGroupMembers(String groupName) throws SQLException {
        List<String> members = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT login FROM group_members WHERE groupName=?");
        ps.setString(1, groupName);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) { members.add(rs.getString("login")); }
        return members;
    }

    public List<String> getGroupsOfUser(String login) throws SQLException {
        List<String> groups = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT groupName FROM group_members WHERE login=?");
        ps.setString(1, login);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) { groups.add(rs.getString("groupName")); }
        return groups;
    }

    public List<String> getAllGroups() throws SQLException {
        List<String> groups = new ArrayList<>();
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery("SELECT groupName FROM groups");
        while (rs.next()) { groups.add(rs.getString("groupName")); }
        return groups;
    }

    // Pendentes do grupo:
    public void addPendingToGroup(String groupName, String login) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO group_pending VALUES (?, ?)");
        ps.setString(1, groupName);
        ps.setString(2, login);
        ps.execute();
    }

    public void removePendingFromGroup(String groupName, String login) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM group_pending WHERE groupName=? AND login=?");
        ps.setString(1, groupName);
        ps.setString(2, login);
        ps.execute();
    }

    public List<String> getGroupPendingInvites(String groupName) throws SQLException {
        List<String> pending = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT login FROM group_pending WHERE groupName=?");
        ps.setString(1, groupName);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) { pending.add(rs.getString("login")); }
        return pending;
    }

    public void saveMessage(Message m) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO messages VALUES (?,?,?,?,?)");
        ps.setString(1, m.getSender());
        ps.setString(2, m.getRecipient());
        ps.setString(3, m.getContent());
        ps.setString(4, m.getTimestamp());
        ps.setInt(5, m.isDelivered() ? 1 : 0);
        ps.execute();
    }

    public List<Message> getUndeliveredMessages(String login) throws SQLException {
        List<Message> msgs = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM messages WHERE recipient=? AND delivered=0");
        ps.setString(1, login);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Message m = new Message(rs.getString("sender"), rs.getString("recipient"), rs.getString("content"));
            msgs.add(m);
        }
        return msgs;
    }

    public void markMessagesAsDelivered(String login) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE messages SET delivered=1 WHERE recipient=?");
        ps.setString(1, login);
        ps.execute();
    }

    // Verifica se o nome já foi usado por outro usuário
    public boolean isNameUsed(String name) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE name=?");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    // Busca usuário pelo email (usado para recuperar senha)
    public User getUserByEmail(String email) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email=?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new User(rs.getString("name"), rs.getString("login"), rs.getString("email"),
                            rs.getString("passwordHash"), rs.getString("status"));
        }
        return null;
    }

    // Atualiza a senha do usuário (usando hash)
    public void updateUserPassword(String login, String novoHash) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE users SET passwordHash=? WHERE login=?");
        ps.setString(1, novoHash);
        ps.setString(2, login);
        ps.execute();
    }

}
