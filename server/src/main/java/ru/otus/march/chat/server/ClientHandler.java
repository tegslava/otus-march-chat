package ru.otus.march.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private final Server server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String nickname;
    private Role role;

    public Socket getSocket() {
        return socket;
    }

    public String getNickname() {
        return nickname;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                System.out.println("Подключился новый клиент");
                if (tryToAuthenticate()) {
                    communicate();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void communicate() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/")) {
                if (msg.startsWith("/exit")) {
                    break;
                } else if (msg.startsWith("/w ")) {
                    String[] tokens = msg.split(" ", 3);
                    if (tokens.length != 3) {
                        sendMessage("Некорректный формат запроса");
                        continue;
                    }
                    String clientName = tokens[1];
                    String msgString = tokens[2];
                    if (!clientName.equals(nickname)) {
                        server.sendMessageToClient(clientName, nickname + " wrote me: " + msgString);
                    }
                } else if (msg.startsWith("/kick ")) {
                    String[] tokens = msg.split(" ");
                    if (tokens.length != 2) {
                        sendMessage("Некорректный формат запроса");
                        continue;
                    }
                    String clientName = tokens[1];
                    if (this.role == Role.ADMIN && !clientName.equals(nickname)) {
                        server.kickClient(clientName);
                    }
                }
                continue;
            }
            server.broadcastMessage(nickname + ": " + msg);
        }
    }

    private boolean tryToAuthenticate() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/auth ")) {
                String[] tokens = msg.split(" ");
                if (tokens.length != 3) {
                    sendMessage("Некорректный формат запроса");
                    continue;
                }
                String login = tokens[1];
                String password = tokens[2];
                String nickname = server.getAuthenticationService().getNicknameByLoginAndPassword(login, password);
                if (nickname == null) {
                    sendMessage("Неправильный логин/пароль");
                    continue;
                }
                if (server.isNicknameBusy(nickname)) {
                    sendMessage("Указанная учетная запись уже занята. Попробуйте зайти позднее");
                    continue;
                }
                this.nickname = nickname;
                this.role = server.getAuthenticationService().getRoleByLoginAndPassword(login, password);
                server.subscribe(this);
                sendMessage(nickname + ", добро пожаловать в чат!");
                return true;
            } else if (msg.startsWith("/register ")) {
                // register login pass nickname
                String[] tokens = msg.split(" ");
                if (tokens.length != 4) {
                    sendMessage("Некорректный формат запроса");
                    continue;
                }
                String login = tokens[1];
                String password = tokens[2];
                String nickname = tokens[3];
                if (server.getAuthenticationService().isLoginAlreadyExist(login)) {
                    sendMessage("Указанный логин уже занят");
                    continue;
                }
                if (server.getAuthenticationService().isNicknameAlreadyExist(nickname)) {
                    sendMessage("Указанный никнейм уже занят");
                    continue;
                }
                if (!server.getAuthenticationService().register(login, password, nickname)) {
                    sendMessage("Не удалось пройти регистрацию");
                    continue;
                }
                this.nickname = nickname;
                server.subscribe(this);
                sendMessage("Вы успешно зарегистрировались! " + nickname + ", добро пожаловать в чат!");
                return true;
            } else if (msg.equals("/exit")) {
                return false;
            } else {
                sendMessage("Вам необходимо авторизоваться");
            }
        }
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
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
