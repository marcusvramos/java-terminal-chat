package core;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            new Thread(() -> {
                String line;
                try {
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("====")) {
                            System.out.println("\u001B[33m" + line + "\u001B[0m");
                        } else if (line.contains("[PRIVADO]")) {
                            System.out.println("\u001B[35m" + line + "\u001B[0m");
                        } else if (line.contains("[Grupo")) {
                            System.out.println("\u001B[34m" + line + "\u001B[0m");
                        } else if (line.contains("Comando não reconhecido") || line.contains("erro") || line.contains("Erro")) {
                            System.out.println("\u001B[31m" + line + "\u001B[0m");
                        } else if (line.contains("Atenção:")) {
                            System.out.println("\u001B[31m" + line + "\u001B[0m");
                        } else {
                            System.out.println(line);
                        }
                    }
                } catch (IOException e) {}
            }).start();

            while (true) {
                System.out.print("\u001B[32mDigite um comando: \u001B[0m");
                String msg = scanner.nextLine().trim();
                if (msg.equalsIgnoreCase("clear")) {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    continue;
                }
                out.println(msg);
                if (msg.equalsIgnoreCase("/sair")) break;
            }

            socket.close();
        } catch (Exception e) {
            System.out.println("\u001B[31mErro de conexão. Certifique-se de que o servidor está rodando.\u001B[0m");
        }
    }
}
