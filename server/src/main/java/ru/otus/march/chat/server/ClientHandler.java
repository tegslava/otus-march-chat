package ru.otus.march.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    private static int usersCounter = 0;

    private void generateUsername() {
        usersCounter++;
        this.username = "user" + usersCounter;
    }

    public String getUsername() {
        return username;
    }

    private static String[] parsingUserCommand(String input) {
        String[] allMatches = new String[3];
        String regex = "^(/w )(\\w*) (.*)";
        Matcher m = Pattern.compile(regex).matcher(input);
        if (m.find()) {
            allMatches[0] = m.group(1);
            allMatches[1] = m.group(2);
            allMatches[2] = m.group(3);
        }
        return allMatches;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.generateUsername();
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/")) {
                        if (msg.startsWith("/exit")) {
                            disconnect();
                            break;
                        } else if (msg.startsWith("/w ")) {
                            String[] parsingWords = parsingUserCommand(msg);
                            String clientName = parsingWords[1].toUpperCase();
                            String msgString = parsingWords[2];
                            server.sendMessageToClient(clientName, this.username + " wrote me: " + msgString);
                        }
                        continue;
                    }
                    server.broadcastMessage(username + ": " + msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null /*&& !socket.isClosed()*/) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
