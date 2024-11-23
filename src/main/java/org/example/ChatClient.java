package org.example;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private static PrintWriter out;
    private static BufferedReader in;
    private static String nickname;
    private static BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private static boolean userListReceived = false;  // флаг для предотвращения дублирования

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Вводим никнейм и отправляем его на сервер
            System.out.print("Введите ваш никнейм: ");
            nickname = consoleReader.readLine();
            out.println(nickname);

            System.out.println("Вы подключились к сети!");

            // Создаем поток для получения сообщений от сервера
            new Thread(ChatClient::receiveMessages).start();

            // Ждем системное сообщение о подключении нового пользователя
            String initialMessage = messageQueue.take();  // Блокирует выполнение до получения сообщения
            System.out.println(initialMessage);

            // Получаем и показываем список пользователей сразу после подключения
            getUserList();
            processQueuedMessages(); // Обрабатываем полученные сообщения (список пользователей)

            // Далее показываем меню для выбора действия
            displayMenu();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();  // Обрабатываем исключения
        }
    }

    private static void displayMenu() throws IOException {
        while (true) {
            System.out.println("\nВыберите действие:");
            System.out.println("    1. Личное сообщение");
            System.out.println("    2. Сообщение для всех");
            System.out.print("Ваш выбор: ");
            String choice = consoleReader.readLine();

            switch (choice) {
                case "1":
                    sendPrivateMessage();
                    break;
                case "2":
                    sendBroadcastMessage();
                    break;
                default:
                    System.out.println("Неверный выбор. Попробуйте снова.");
            }

            // Обрабатываем любые полученные сообщения во время ввода
            try {
                processQueuedMessages();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // Восстанавливаем флаг прерывания
                e.printStackTrace();
            }
        }
    }

    private static void sendPrivateMessage() throws IOException {
        // Сначала выводим список пользователей
        getUserList();
        try {
            processQueuedMessages();  // Обрабатываем полученные сообщения (список пользователей)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Восстанавливаем флаг прерывания
            e.printStackTrace();
        }

        // Затем даем выбрать получателя
        System.out.print("Введите никнейм получателя: ");
        String recipient = consoleReader.readLine();
        System.out.print("Введите сообщение: ");
        String message = consoleReader.readLine();
        out.println("PRIVATE:" + recipient + ":" + message);
    }

    private static void sendBroadcastMessage() throws IOException {
        System.out.print("Введите сообщение для всех: ");
        String message = consoleReader.readLine();
        out.println("BROADCAST:" + message);
    }

    // Этот метод будет сразу вызван после подключения
    private static void getUserList() {
        out.println("GET_USERS");
    }

    private static void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                try {
                    messageQueue.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();  // Восстанавливаем флаг прерывания
                    e.printStackTrace();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processQueuedMessages() throws InterruptedException {
        while (!messageQueue.isEmpty()) {
            String message = messageQueue.take();
            String[] parts = message.split(":", 3);
            String messageType = parts[0];

            switch (messageType) {
                case "USERS":
                    if (!userListReceived) {  // Если список пользователей еще не был выведен
                        displayUserList(parts[1]);
                        userListReceived = true;  // Устанавливаем флаг, чтобы не выводить список снова
                    }
                    break;
                case "PRIVATE":
                    System.out.println("\nЛичное сообщение от " + parts[1] + ": " + parts[2]);
                    break;
                case "BROADCAST":
                    System.out.println("\nСообщение всем от " + parts[1] + ": " + parts[2]);
                    break;
                case "SYSTEM":
                    System.out.println("\nСистемное сообщение: " + parts[1]);
                    break;
                default:
                    System.out.println("\nПолучено неизвестное сообщение: " + message);
            }
        }
    }

    private static void displayUserList(String userListString) {
        String[] users = userListString.split(",");
        System.out.println("\nПодключенные пользователи:");
        for (String user : users) {
            System.out.println("    - " + user);
        }
    }
}
