package core;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import db.Database;
import entities.*;
import service.Utils;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private User user;

    private Map<String, String> groupInvites = new HashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void printMenu() {
        out.println();
        out.println("=== Comandos disponíveis ===");
        out.println("/listarusuarios                    → Lista usuários online");
        out.println("/listargrupos                      → Lista todos os grupos");
        out.println("/criargroup nome                   → Cria grupo");
        out.println("/adicionar login grupo             → Convida usuário para grupo");
        out.println("/listarmembros grupo               → Lista membros do grupo");
        out.println("/sairgrupo grupo                   → Sai do grupo");
        out.println("/status online/ocupado             → Muda status");
        out.println("/msg login,maria texto             → Msg privada para 1 ou + usuários (precisa aceite)");
        out.println("/msggroup grupo mensagem           → Msg para TODOS do grupo (exceto você)");
        out.println("/msggroup grupo robson@Olá         → Msg só para Robson no grupo");
        out.println("/msggroup grupo robson,sisc@Oi     → Msg só para esses usuários no grupo");
        out.println("/aceitar login                     → Aceitar chat privado");
        out.println("/recuperar                         → Recuperar senha por email");
        out.println("/aceitargrupo grupo                → Aceitar convite/entrada em grupo");
        out.println("/recusargrupo grupo                → Recusar convite/entrada em grupo");
        out.println("/solicitarentrada grupo            → Pedir entrada (todos membros devem aceitar)");
        out.println("/aceitarentrada grupo login        → Aceitar entrada do usuário no grupo");
        out.println("/recusarentrada grupo login        → Recusar entrada do usuário no grupo");
        out.println("/sair                              → Sair do chat");
        out.println("========================================================================================\n");
    }

    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            Utils.printSeparator();
            out.println(" Bem-vindo ao CHAT TERMINAL!\nDigite /cadastro, /login ou /recuperar para senha.");
            Utils.printSeparator();

            boolean authenticated = false;
            while (!authenticated) {
                String input = in.readLine();
                if (input == null) break;
                if (input.startsWith("/cadastro")) {
                    authenticated = handleCadastro();
                } else if (input.startsWith("/login")) {
                    authenticated = handleLogin();
                } else if (input.startsWith("/recuperar")) {
                    recuperarSenha();
                } else {
                    out.println("Comando inválido. Use /cadastro, /login ou /recuperar");
                }
            }

            if (authenticated) {
                ChatServer.onlineClients.put(user.getLogin(), this);
                ChatServer.db.updateUserStatus(user.getLogin(), "online");
                Utils.printSeparator();
                out.println("Login realizado!\nBem-vindo, " + user.getName() + "!");
                sendUndeliveredMessages();
                printMenu();
                mainLoop();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            try {
                if (user != null) {
                    ChatServer.onlineClients.remove(user.getLogin());
                    ChatServer.db.updateUserStatus(user.getLogin(), "offline");
                }
                if (socket != null) socket.close();
            } catch (Exception ex) {}
        }
    }

    private boolean handleCadastro() throws IOException, SQLException {
        out.println("Nome completo:");
        String name = in.readLine().trim();
        out.println("Login (mínimo 3 letras, sem espaços):");
        String login = in.readLine().toLowerCase().trim();
        out.println("Email:");
        String email = in.readLine().trim();
        out.println("Senha:");
        String senha = in.readLine().trim();

        if (login.contains(" ") || login.length() < 3) {
            out.println("Login inválido.");
            return false;
        }
        if (ChatServer.db.getUserByLogin(login) != null) {
            out.println("Login já existe!");
            return false;
        }
        // NOME ÚNICO
        if (ChatServer.db.isNameUsed(name)) {
            out.println("Nome já está em uso! Escolha outro nome.");
            return false;
        }
        String hash = Utils.hashPassword(senha);
        User novo = new User(name, login, email, hash, "offline");
        if (ChatServer.db.registerUser(novo)) {
            out.println("Usuário cadastrado com sucesso! Faça login:");
            return false;
        } else {
            out.println("Erro ao cadastrar usuário.");
            return false;
        }
    }

    private boolean handleLogin() throws IOException, SQLException {
        out.println("Login:");
        String login = in.readLine().toLowerCase().trim();
        out.println("Senha:");
        String senha = in.readLine();

        if (ChatServer.onlineClients.containsKey(login)) {
            out.println("Usuário já está logado em outro terminal.");
            return false;
        }
        User u = ChatServer.db.getUserByLogin(login);
        if (u == null) {
            out.println("Usuário não encontrado.");
            return false;
        }
        if (!u.getPasswordHash().equals(Utils.hashPassword(senha))) {
            out.println("Senha incorreta.");
            return false;
        }
        this.user = u;
        return true;
    }

    private void recuperarSenha() throws IOException, SQLException {
        out.println("Informe o nome completo cadastrado:");
        String nome = in.readLine().trim();
        out.println("Informe o email cadastrado:");
        String email = in.readLine().trim();
        User found = ChatServer.db.getUserByEmail(email);
        if (found != null && found.getName().equalsIgnoreCase(nome)) {
            out.println("Dados localizados! Digite sua nova senha:");
            String novaSenha = in.readLine().trim();
            // Salva a nova senha como hash (recomendado)
            String novoHash = Utils.hashPassword(novaSenha);
            ChatServer.db.updateUserPassword(found.getLogin(), novoHash);
            out.println("Senha redefinida com sucesso! Agora faça login com a nova senha.");
        } else {
            out.println("Nome e e-mail não encontrados juntos. Confira os dados.");
        }
    }



    private void mainLoop() throws IOException, SQLException {
        String input;
        while ((input = in.readLine()) != null) {
            input = input.trim();
            if (input.equalsIgnoreCase("/sair")) {
                out.println("Até logo, " + user.getName() + "!");
                break;
            } else if (input.startsWith("/status")) {
                String[] p = input.split(" ");
                if (p.length > 1) {
                    ChatServer.db.updateUserStatus(user.getLogin(), p[1]);
                    user.setStatus(p[1]);
                    out.println("Status alterado para: " + p[1]);

                    if (p[1].equalsIgnoreCase("online")) {
                        sendUndeliveredMessages();
                    }
                }
            } else if (input.startsWith("/listarusuarios")) {
                List<User> users = ChatServer.db.getOnlineUsers();
                out.println("\nUsuários online:");
                for (User u : users) {
                    out.println(" - " + u.getName() + " (" + u.getLogin() + ")");
                }
            } else if (input.startsWith("/listargrupos")) {
                List<String> groups = ChatServer.db.getAllGroups();
                out.println("\nGrupos existentes:");
                for (String g : groups) out.println(" - " + g);
            } else if (input.startsWith("/listarmembros")) {
                String[] p = input.split(" ");
                if (p.length > 1) {
                    List<String> members = ChatServer.db.getGroupMembers(p[1].toLowerCase());
                    if (!members.contains(user.getLogin())) {
                        out.println("Você não tem permissão para ver os membros desse grupo.");
                    } else {
                        out.println("Membros do grupo " + p[1] + ": " + members);
                    }
                }
            } else if (input.startsWith("/criargroup")) {
                String[] p = input.split(" ");
                if (p.length > 1 && ChatServer.db.createGroup(p[1].toLowerCase())) {
                    ChatServer.db.addMemberToGroup(p[1].toLowerCase(), user.getLogin());
                    out.println("Grupo criado e você foi adicionado.");
                } else {
                    out.println("Erro ao criar grupo ou nome já existe.");
                }
            } else if (input.startsWith("/adicionar")) {
                String[] p = input.split(" ");
                if (p.length > 2 && ChatServer.db.groupExists(p[2].toLowerCase())) {
                    String convidado = p[1].toLowerCase();
                    if (ChatServer.db.getUserByLogin(convidado) == null) {
                        out.println("Usuário não existe.");
                    } else if (ChatServer.db.getGroupMembers(p[2].toLowerCase()).contains(convidado)) {
                        out.println("Usuário já está no grupo.");
                    } else {
                        ChatServer.db.addPendingToGroup(p[2].toLowerCase(), convidado);
                        out.println("Convite enviado para " + convidado + ".");
                        if (ChatServer.onlineClients.containsKey(convidado)) {
                            ChatServer.onlineClients.get(convidado).out.println("Você foi convidado para o grupo '" + p[2].toLowerCase() + "'. Digite /aceitargrupo " + p[2].toLowerCase() + " ou /recusargrupo " + p[2].toLowerCase());
                        }
                    }
                } else {
                    out.println("Grupo não existe.");
                }
            } else if (input.startsWith("/aceitargrupo")) {
                String[] p = input.split(" ");
                if (p.length > 1 && ChatServer.db.groupExists(p[1].toLowerCase())) {
                    List<String> pendings = ChatServer.db.getGroupPendingInvites(p[1].toLowerCase());
                    if (pendings.contains(user.getLogin())) {
                        ChatServer.db.removePendingFromGroup(p[1].toLowerCase(), user.getLogin());
                        ChatServer.db.addMemberToGroup(p[1].toLowerCase(), user.getLogin());
                        out.println("Você agora faz parte do grupo '" + p[1].toLowerCase() + "'!");
                        for (String m : ChatServer.db.getGroupMembers(p[1].toLowerCase())) {
                            if (!m.equals(user.getLogin()) && ChatServer.onlineClients.containsKey(m)) {
                                ChatServer.onlineClients.get(m).out.println(user.getLogin() + " entrou no grupo '" + p[1].toLowerCase() + "'.");
                            }
                        }
                    } else {
                        out.println("Você não possui convite pendente para esse grupo.");
                    }
                } else {
                    out.println("Grupo não existe.");
                }
            } else if (input.startsWith("/recusargrupo")) {
                String[] p = input.split(" ");
                if (p.length > 1 && ChatServer.db.groupExists(p[1].toLowerCase())) {
                    List<String> pendings = ChatServer.db.getGroupPendingInvites(p[1].toLowerCase());
                    if (pendings.contains(user.getLogin())) {
                        ChatServer.db.removePendingFromGroup(p[1].toLowerCase(), user.getLogin());
                        out.println("Convite recusado para grupo '" + p[1].toLowerCase() + "'.");
                    } else {
                        out.println("Você não possui convite pendente para esse grupo.");
                    }
                } else {
                    out.println("Grupo não existe.");
                }
            } else if (input.startsWith("/sairgrupo")) {
                String[] p = input.split(" ");
                if (p.length > 1) {
                    String grupo = p[1].toLowerCase();
                    if (!ChatServer.db.getGroupMembers(grupo).contains(user.getLogin())) {
                        out.println("Você não faz parte deste grupo.");
                    } else {
                        ChatServer.db.removeMemberFromGroup(grupo, user.getLogin());
                        out.println("Você saiu do grupo " + grupo + ".");
                        for (String m : ChatServer.db.getGroupMembers(grupo)) {
                            if (ChatServer.onlineClients.containsKey(m)) {
                                ChatServer.onlineClients.get(m).out.println(user.getLogin() + " saiu do grupo '" + grupo + "'.");
                            }
                        }
                    }
                }
            } else if (input.startsWith("/msggroup")) {
                // /msggroup grupo robson@Oi! ou robson,sisc@Oi!
                String[] p = input.split(" ", 3);
                if (p.length < 3) {
                    out.println("Uso: /msggroup grupo usuario@texto");
                    continue;
                }
                sendMessageToSelectedInGroup(p[1].toLowerCase(), p[2]);
            } else if (input.startsWith("/msg")) {
                String[] p = input.split(" ", 3);
                if (p.length < 3) {
                    out.println("Uso: /msg login mensagem");
                    continue;
                }
                String destinos = p[1];
                String texto = p[2];
                for (String destino : destinos.split(",")) {
                    destino = destino.trim().toLowerCase();
                    if (destino.equals(user.getLogin())) continue;
                    User destUser = ChatServer.db.getUserByLogin(destino);
                    if (destUser == null) {
                        out.println("Usuário " + destino + " não encontrado.");
                        continue;
                    }

                    // VERIFICA SE JÁ FOI AUTORIZADO O CHAT PRIVADO ENTRE OS USUÁRIOS
                    Set<String> autorizados = ChatServer.allowedPrivateChats.getOrDefault(user.getLogin(), new HashSet<>());
                    if (autorizados.contains(destino)) {
                        // Entrega mensagem normalmente (respeitando status online/ocupado/offline)
                        String destStatus = destUser.getStatus().toLowerCase();
                        Message m = new Message(user.getLogin(), destino, texto);
                        if (destStatus.equals("online") && ChatServer.onlineClients.containsKey(destino)) {
                            ChatServer.onlineClients.get(destino).out.println("[PRIVADO] " + user.getLogin() + " (" + m.getTimestamp() + "): " + texto);
                            m.setDelivered(true);
                            out.println("Mensagem entregue para " + destino + ".");
                        } else {
                            out.println("Usuário " + destino + " está '" + destStatus + "'. Mensagem será entregue quando ficar online.");
                        }
                        ChatServer.db.saveMessage(m);
                    } else {
                        // NÃO AUTORIZADO: Envia pedido de chat privado e armazena mensagem pendente
                        if (ChatServer.onlineClients.containsKey(destino)) {
                            ChatServer.pendingPrivateRequests.putIfAbsent(destino, new HashSet<>());
                            ChatServer.pendingPrivateRequests.get(destino).add(user.getLogin());
                            ChatServer.pendingPrivateMessages.put(destino + ":" + user.getLogin(), texto);
                            ChatServer.onlineClients.get(destino).out.println("Usuário '" + user.getLogin() + "' deseja iniciar um chat privado com você. Aceitar? (/aceitar " + user.getLogin() + ")");
                            out.println("Pedido de chat privado enviado a " + destino + ". Aguarde aceite.");
                        } else {
                            out.println("Usuário " + destino + " está offline. Mensagem não pode ser enviada sem aceite.");
                        }
                    }
                }
            } else if (input.startsWith("/aceitar ")) {
                String[] p = input.split(" ");
                if (p.length > 1) {
                    acceptPrivateChat(p[1].toLowerCase());
                }
            } else if (input.startsWith("/solicitarentrada")) {
                String[] p = input.split(" ");
                if (p.length > 1) {
                    solicitarEntradaGrupo(p[1].toLowerCase());
                }
            } else if (input.startsWith("/aceitarentrada")) {
                String[] p = input.split(" ");
                if (p.length > 2) {
                    aceitarEntradaGrupo(p[1].toLowerCase(), p[2].toLowerCase());
                }
            } else if (input.startsWith("/recusarentrada")) {
                String[] p = input.split(" ");
                if (p.length > 2) {
                    recusarEntradaGrupo(p[1].toLowerCase(), p[2].toLowerCase());
                }
            } else {
                out.println("Comando não reconhecido. Digite um comando válido.");
                printMenu();
            }
        }
    }

    // Aceite para chat privado (com entrega da mensagem pendente)
    private void requestPrivateChat(String destino, String texto) throws SQLException {
        if (destino.equals(user.getLogin())) {
            out.println("Você não pode enviar mensagem para si mesmo.");
            return;
        }
        User dest = ChatServer.db.getUserByLogin(destino);
        if (dest == null) {
            out.println("Usuário não encontrado.");
            return;
        }

        Set<String> autorizados = ChatServer.allowedPrivateChats.getOrDefault(user.getLogin(), new HashSet<>());
        if (autorizados.contains(destino)) {
            // Já autorizado, pode mandar msg
            Message m = new Message(user.getLogin(), destino, texto);
            if (ChatServer.onlineClients.containsKey(destino)) {
                ChatServer.onlineClients.get(destino).out.println("[PRIVADO] " + user.getLogin() + " (" + m.getTimestamp() + "): " + texto);
                m.setDelivered(true);
                out.println("Mensagem entregue para " + destino + ".");
            } else {
                out.println("Usuário " + destino + " está offline. Mensagem será entregue quando ele voltar.");
            }
            ChatServer.db.saveMessage(m);
        } else {
            // Solicita permissão primeiro
            if (ChatServer.onlineClients.containsKey(destino)) {
                ChatServer.pendingPrivateRequests.putIfAbsent(destino, new HashSet<>());
                ChatServer.pendingPrivateRequests.get(destino).add(user.getLogin());
                // Salva a mensagem pendente
                ChatServer.pendingPrivateMessages.put(destino + ":" + user.getLogin(), texto);
                ChatServer.onlineClients.get(destino).out.println("Usuário '" + user.getLogin() + "' deseja iniciar um chat privado com você. Aceitar? (/aceitar " + user.getLogin() + ")");
                out.println("Pedido de chat privado enviado. Aguarde aceite.");
            } else {
                out.println("Usuário está offline. Mensagem não pode ser enviada sem aceite.");
            }
        }
    }

    // Aceita chat privado e já entrega a primeira mensagem pendente!
    private void acceptPrivateChat(String quem) throws SQLException {
        Set<String> pedidos = ChatServer.pendingPrivateRequests.getOrDefault(user.getLogin(), new HashSet<>());
        if (pedidos.contains(quem)) {
            ChatServer.allowedPrivateChats.putIfAbsent(user.getLogin(), new HashSet<>());
            ChatServer.allowedPrivateChats.putIfAbsent(quem, new HashSet<>());
            ChatServer.allowedPrivateChats.get(user.getLogin()).add(quem);
            ChatServer.allowedPrivateChats.get(quem).add(user.getLogin());
            pedidos.remove(quem);
            out.println("Você agora pode conversar com '" + quem + "'.");

            // Entrega a mensagem pendente imediatamente, se houver!
            String chave = user.getLogin() + ":" + quem;
            if (ChatServer.pendingPrivateMessages.containsKey(chave)) {
                String texto = ChatServer.pendingPrivateMessages.get(chave);
                Message m = new Message(quem, user.getLogin(), texto);
                out.println("[PRIVADO] " + quem + " (" + m.getTimestamp() + "): " + texto);
                ChatServer.pendingPrivateMessages.remove(chave);
                ChatServer.db.saveMessage(m);
            }

            if (ChatServer.onlineClients.containsKey(quem)) {
                ChatServer.onlineClients.get(quem).out.println("Usuário '" + user.getLogin() + "' aceitou seu pedido de chat privado! Agora podem conversar.");
            }
        } else {
            out.println("Não há pedido pendente de '" + quem + "'.");
        }
    }

    // Envia mensagem só para alguns usuários do grupo ou para todos
    private void sendMessageToSelectedInGroup(String group, String content) throws SQLException {
        if (!ChatServer.db.groupExists(group)) {
            out.println("Grupo não existe.");
            return;
        }
        List<String> members = ChatServer.db.getGroupMembers(group);
        if (!members.contains(user.getLogin())) {
            out.println("Você não faz parte do grupo.");
            return;
        }
        // Se content não tiver @, manda pra todos
        if (!content.contains("@")) {
            for (String dest : members) {
                if (dest.equals(user.getLogin())) continue;
                User userDest = ChatServer.db.getUserByLogin(dest);
                if (userDest == null) continue;
                Message m = new Message(user.getLogin(), dest, "[Grupo " + group + "] " + content);
                if (ChatServer.onlineClients.containsKey(dest)) {
                    ChatServer.onlineClients.get(dest).out.println("[Grupo " + group + "] " + user.getLogin() + " (" + m.getTimestamp() + "): " + content);
                    m.setDelivered(true);
                }
                ChatServer.db.saveMessage(m);
            }
            out.println("Mensagem enviada a todos do grupo.");
        } else {
            // Caso clássico: nomes@login1,login2@Mensagem
            String[] partes = content.split("@", 2);
            List<String> destinatarios = Arrays.asList(partes[0].split(","));
            String texto = partes[1];
            boolean enviado = false;
            for (String dest : destinatarios) {
                dest = dest.trim();
                if (!members.contains(dest) || dest.equals(user.getLogin())) continue;
                User userDest = ChatServer.db.getUserByLogin(dest);
                if (userDest == null) continue;
                Message m = new Message(user.getLogin(), dest, "[Grupo " + group + "] " + texto);
                if (ChatServer.onlineClients.containsKey(dest)) {
                    ChatServer.onlineClients.get(dest).out.println("[Grupo " + group + "] " + user.getLogin() + " (" + m.getTimestamp() + "): " + texto);
                    m.setDelivered(true);
                    enviado = true;
                }
                ChatServer.db.saveMessage(m);
            }
            if (enviado)
                out.println("Mensagem enviada aos destinatários selecionados no grupo.");
            else
                out.println("Ninguém do grupo está online para receber agora.");
        }
    }

    // Solicitação de entrada em grupo (precisa aceite de todos)
    private void solicitarEntradaGrupo(String grupo) throws SQLException {
        if (!ChatServer.db.groupExists(grupo)) {
            out.println("Grupo não existe.");
            return;
        }
        List<String> membros = ChatServer.db.getGroupMembers(grupo);
        if (membros.contains(user.getLogin())) {
            out.println("Você já faz parte desse grupo.");
            return;
        }
        // Inicializa estrutura de controle do pedido
        ChatServer.pendingGroupJoinRequests.putIfAbsent(grupo, new HashMap<>());
        ChatServer.pendingGroupJoinRequests.get(grupo).put(user.getLogin(), new HashSet<>());
        for (String m : membros) {
            if (ChatServer.onlineClients.containsKey(m)) {
                ChatServer.onlineClients.get(m).out.println("Usuário '" + user.getLogin() + "' deseja entrar no grupo '" + grupo + "'. Aceitar? (/aceitarentrada " + grupo + " " + user.getLogin() + ")");
            }
        }
        out.println("Pedido de entrada enviado aos membros do grupo. Aguarde todos aceitarem.");
    }

    private void aceitarEntradaGrupo(String grupo, String quem) throws SQLException {
        if (!ChatServer.db.groupExists(grupo)) return;
        List<String> membros = ChatServer.db.getGroupMembers(grupo);
        if (!membros.contains(user.getLogin())) {
            out.println("Só membros do grupo podem aceitar novos participantes.");
            return;
        }
        if (!ChatServer.pendingGroupJoinRequests.containsKey(grupo) || !ChatServer.pendingGroupJoinRequests.get(grupo).containsKey(quem)) {
            out.println("Não há pedido pendente de " + quem + " para esse grupo.");
            return;
        }
        ChatServer.pendingGroupJoinRequests.get(grupo).get(quem).add(user.getLogin());
        if (ChatServer.pendingGroupJoinRequests.get(grupo).get(quem).size() == membros.size()) {
            // Todos aceitaram!
            ChatServer.db.addMemberToGroup(grupo, quem);
            if (ChatServer.onlineClients.containsKey(quem)) {
                ChatServer.onlineClients.get(quem).out.println("Sua solicitação de entrada foi aceita por todos e você entrou no grupo '" + grupo + "'!");
            }
            for (String m : membros) {
                if (ChatServer.onlineClients.containsKey(m)) {
                    ChatServer.onlineClients.get(m).out.println("Usuário '" + quem + "' entrou no grupo '" + grupo + "' após aprovação de todos.");
                }
            }
            ChatServer.pendingGroupJoinRequests.get(grupo).remove(quem);
        } else {
            out.println("Aceite registrado! Aguarde todos aceitarem.");
        }
    }

    private void recusarEntradaGrupo(String grupo, String quem) throws SQLException {
        if (!ChatServer.db.groupExists(grupo)) return;
        List<String> membros = ChatServer.db.getGroupMembers(grupo);
        if (!membros.contains(user.getLogin())) {
            out.println("Só membros do grupo podem recusar novos participantes.");
            return;
        }
        if (!ChatServer.pendingGroupJoinRequests.containsKey(grupo) || !ChatServer.pendingGroupJoinRequests.get(grupo).containsKey(quem)) {
            out.println("Não há pedido pendente de " + quem + " para esse grupo.");
            return;
        }
        if (ChatServer.onlineClients.containsKey(quem)) {
            ChatServer.onlineClients.get(quem).out.println("Sua solicitação de entrada foi recusada por '" + user.getLogin() + "'. Você não entrou no grupo.");
        }
        ChatServer.pendingGroupJoinRequests.get(grupo).remove(quem);
        out.println("Solicitação recusada.");
    }

    private void sendUndeliveredMessages() throws SQLException {
        List<Message> msgs = ChatServer.db.getUndeliveredMessages(user.getLogin());
        if (msgs.isEmpty()) return;
        out.println("==== Mensagens não lidas ====");
        for (Message m : msgs) {
            out.println(m.getSender() + " (" + m.getTimestamp() + "): " + m.getContent());
        }
        ChatServer.db.markMessagesAsDelivered(user.getLogin());
        out.println("=============================");
    }
}
