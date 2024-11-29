package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NetworkHandler {
    private PrintWriter out;
    private BufferedReader in;
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private String nickname;

    // Конструктор, принимающий объект Socket
    public NetworkHandler(Socket socket) {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            startReceivingMessages();
        } catch (IOException e) {
            System.out.println(ChatClient.RED + "Ошибка подключения: " + e.getMessage() + ChatClient.RESET);
        }
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        out.println(nickname);
    }

    public void sendPrivateMessage(String recipient, String message) {
        out.println("PRIVATE:" + recipient + ":" + message);
    }

    public void sendBroadcastMessage(String message) {
        out.println("BROADCAST:" + message);
    }

    public void requestUserList() {
        out.println("GET_USERS");
    }

    public BlockingQueue<String> getMessageQueue() {
        return messageQueue;
    }

    private void startReceivingMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("USERS:")) {
                        messageQueue.add(message);
                    } else {
                        messageQueue.add(message); // Добавляем другие типы сообщений
                    }
                }
            } catch (IOException e) {
                System.out.println(ChatClient.RED + "Ошибка получения сообщений: " + e.getMessage() + ChatClient.RESET);
            }
        }).start();
    }

    public String getNickname() {
        return nickname; // Возвращает никнейм пользователя
    }
}
