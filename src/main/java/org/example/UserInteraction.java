package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInteraction {
    private static BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private NetworkHandler networkHandler;

    public UserInteraction(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    public void start() {
        try {
            System.out.print(ChatClient.GREEN + "Введите ваш никнейм: " + ChatClient.RESET);
            String nickname = consoleReader.readLine();
            networkHandler.setNickname(nickname);
            System.out.println(ChatClient.GREEN + "Вы подключились к сети!" + ChatClient.RESET);
            displayMenu();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayMenu() throws IOException {
        while (true) {
            processMessages();
            System.out.println("\n===============================");
            System.out.println("Выберите действие:");
            System.out.println("    ➤ 1. Личное сообщение");
            System.out.println("    ➤ 2. Сообщение для всех");
            System.out.print("Ваш выбор: ");
            String choice = consoleReader.readLine();

            switch (choice) {
                case "1":
                    networkHandler.requestUserList(); // Запросить список пользователей перед отправкой сообщения
                    sendPrivateMessage();
                    break;
                case "2":
                    sendBroadcastMessage();
                    break;
                default:
                    System.out.println(ChatClient.RED + "Неверный выбор. Попробуйте снова." + ChatClient.RESET);
            }
        }
    }

    private void processMessages() throws IOException {
        while (!networkHandler.getMessageQueue().isEmpty()) {
            String newMessage = networkHandler.getMessageQueue().poll();
            if (newMessage != null) {
                System.out.println("Получено сообщение: " + newMessage); // Для отладки
                if (newMessage.startsWith("PRIVATE:")) {
                    String[] parts = newMessage.split(":", 3);
                    String sender = parts[1];
                    String privateMessage = parts[2];
                    System.out.println(ChatClient.YELLOW + "Личное сообщение от " + sender + ": " + privateMessage + ChatClient.RESET);
                } else if (newMessage.startsWith("BROADCAST:")) {
                    System.out.println(ChatClient.CYAN + "Сообщение для всех: " + newMessage.substring(10) + ChatClient.RESET);
                } else if (newMessage.startsWith("SYSTEM:")) {
                    System.out.println(newMessage.substring(7));
                } else if (newMessage.startsWith("USERS:")) {
                    System.out.println(ChatClient.GREEN + "Получен список пользователей: " + newMessage.substring(6) + ChatClient.RESET);
                }
            }
        }
    }

    private void sendPrivateMessage() throws IOException {
        // Запрашиваем список пользователей перед отправкой личного сообщения
        networkHandler.requestUserList();

        // Выводим список пользователей, если он был получен
        processMessages(); // Обрабатываем сообщения, чтобы вывести список пользователей

        System.out.print("Введите никнейм получателя: ");
        String recipient = consoleReader.readLine();
        System.out.print("Введите сообщение: ");
        String message = consoleReader.readLine();
        networkHandler.sendPrivateMessage(recipient, message);
        logSentMessage("личное сообщение", recipient, message);
    }

    private String processUserList() throws IOException {
        String userListMessage = null;
        // Проверяем очередь сообщений на наличие списка пользователей
        while (!networkHandler.getMessageQueue().isEmpty()) {
            String newMessage = networkHandler.getMessageQueue().poll();
            if (newMessage.startsWith("USERS:")) {
                userListMessage = newMessage.substring(6);
            }
        }
        return userListMessage;
    }
    private void sendBroadcastMessage() throws IOException {
        System.out.print("Введите сообщение для всех: ");
        String message = consoleReader.readLine();
        networkHandler.sendBroadcastMessage(message);
        logSentMessage("сообщение для всех", null, message);
    }

    private void logSentMessage(String type, String recipient, String message) {
        System.out.println("\n" + ChatClient.YELLOW + "Отправлено " + type + ":" + ChatClient.RESET);
        System.out.println("От: " + networkHandler.getNickname() + " (вы)");
        if (recipient != null) {
            System.out.println("Кому: " + recipient);
        }
        System.out.println("Сообщение: " + message);
    }
}