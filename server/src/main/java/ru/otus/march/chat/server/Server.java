package ru.otus.march.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private final int port;
    private final List<ClientHandler> clients;
    private AuthenticationService authenticationService;

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            //authenticationService = new InMemoryAuthenticationService();
            authenticationService = new SQLiteAuthenticationService();
            System.out.println("Сервис аутентификации запущен: " + authenticationService.getClass().getSimpleName());
            System.out.printf("Сервер запущен на порту: %d, ожидаем подключения клиентов\n", port);
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new ClientHandler(this, socket);
                } catch (Exception e) {
                    System.out.println("Возникла ошибка при обработке подключившегося клиента");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                authenticationService.disconnect();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("К чату присоединился " + clientHandler.getNickname());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Из чата вышел " + clientHandler.getNickname());
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    public synchronized boolean isNicknameBusy(String nickname) {
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendMessageToClient(String nickname, String message) {
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(nickname)) {
                c.sendMessage(message);
                break;
            }
        }
    }

    public synchronized void kickClient(String nickname) {
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(nickname)) {
                try {
                    broadcastMessage(nickname + " забанен администратором ");
                    c.getSocket().close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    getAuthenticationService().unRegister(nickname);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                clients.remove(c);
                return;
            }
        }
    }
}
