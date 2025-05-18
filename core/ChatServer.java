package core;
import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import db.Database;
import entities.*;
import service.Utils;

public class ChatServer {
    public static Map<String, ClientHandler> onlineClients = new HashMap<>(); // login -> handler
    public static Map<String, Set<String>> pendingPrivateRequests = new HashMap<>(); // destino -> quem pediu chat
    public static Map<String, String> pendingPrivateMessages = new HashMap<>(); // chave: destino:remetente, valor: msg
    public static Map<String, Set<String>> allowedPrivateChats = new HashMap<>(); // login -> quem já pode conversar

    // Gerenciamento de pedidos de entrada em grupo: grupo -> (login -> status)
    public static Map<String, Map<String, Set<String>>> pendingGroupJoinRequests = new HashMap<>();
    // Ex: pendingGroupJoinRequests.get("amigos").get("pedro") = set de logins de quem já aceitou

    public static Database db;

    public static void main(String[] args) {
        try {
            db = new Database("chat.db");
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("\n==========================================");
            System.out.println("    Servidor de Chat iniciado na porta 12345");
            System.out.println("==========================================\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}
