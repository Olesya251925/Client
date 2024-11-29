package org.example;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.*;

public class ChatClient {
    private static String SERVER_HOST;
    private static int SERVER_PORT;
    private static BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private static PrintWriter out;
    private static BufferedReader in;
    private static String nickname;
    private static BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    // ANSI Escape Codes для цветного текста
    public static final String RESET = "\033[0m";  // Сброс цвета
    public static final String RED = "\033[31m";    // Красный
    public static final String GREEN = "\033[32m";  // Зеленый
    public static final String YELLOW = "\033[33m"; // Желтый
    public static final String CYAN = "\033[36m";   // Циан

    static {
        // Загружаем параметры из файла config.properties
        try (InputStream input = ChatClient.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (input == null) {
                System.out.println(RED + "Не удалось найти файл client.properties" + RESET);
            } else {
                Properties prop = new Properties();
                prop.load(input);

                SERVER_HOST = prop.getProperty("server.host", "localhost"); // По умолчанию localhost
                SERVER_PORT = Integer.parseInt(prop.getProperty("server.port", "12345")); // По умолчанию 12345
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Вводим никнейм и отправляем его на сервер
            System.out.print(GREEN + "Введите ваш никнейм: " + RESET);
            nickname = consoleReader.readLine();
            out.println(nickname);

            System.out.println(GREEN + "Вы подключились к сети!" + RESET);

            // Создаем поток для получения сообщений от сервера
            new Thread(ChatClient::receiveMessages).start();

            // Теперь сразу показываем меню
            displayMenu();

        } catch (IOException e) {
            System.out.println(RED + "Ошибка подключения: " + e.getMessage() + RESET);
        }
    }

    private static void displayMenu() throws IOException {
        while (true) {
            // Проверяем наличие новых сообщений
            while (!messageQueue.isEmpty()) {
                String newMessage = messageQueue.poll();
                if (newMessage != null) {
                    System.out.println("\n" + CYAN + newMessage + RESET); // Цветное сообщение
                }
            }

            System.out.println("\n===============================");
            System.out.println("Выберите действие:");
            System.out.println("    ➤ 1. Личное сообщение");
            System.out.println("    ➤ 2. Сообщение для всех");
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
                    System.out.println(RED + "Неверный выбор. Попробуйте снова." + RESET);
            }
        }
    }

    private static void sendPrivateMessage() throws IOException {
        getUserList();  // Получаем список пользователей перед отправкой личного сообщения
        System.out.print("Введите никнейм получателя: ");
        String recipient = consoleReader.readLine();
        System.out.print("Введите сообщение: ");
        String message = consoleReader.readLine();
        out.println("PRIVATE:" + recipient + ":" + message);

        // Логируем отправку личного сообщения
        System.out.println("\n" + YELLOW + "Отправлено личное сообщение:" + RESET);
        System.out.println("От: " + nickname + " (вы)");
        System.out.println("Кому: " + recipient);
        System.out.println("Сообщение: " + message);
    }

    private static void sendBroadcastMessage() throws IOException {
        System.out.print("Введите сообщение для всех: ");
        String message = consoleReader.readLine();
        out.println("BROADCAST:" + message);

        // Логируем отправку общего сообщения
        System.out.println("\n" + YELLOW + "Отправлено сообщение для всех:" + RESET);
        System.out.println("Сообщение: " + message);
    }

    private static void getUserList() {
        out.println("GET_USERS");

        // Ждем немного, чтобы получить список пользователей
        try {
            Thread.sleep(100);
            String userListMessage = messageQueue.poll();
            if (userListMessage != null && userListMessage.startsWith("USERS:")) {
                System.out.println(GREEN + "Получен список пользователей: " + userListMessage.substring(6) + RESET);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("PRIVATE:")) {
                    String[] parts = message.split(":", 3);
                    String sender = parts[1];
                    String privateMessage = parts[2];
                    System.out.println(YELLOW + "Личное сообщение от " + sender + ": " + privateMessage + RESET);
                } else if (message.startsWith("BROADCAST:")) {
                    // Выводим общее сообщение
                    System.out.println(CYAN + "Сообщение для всех: " + message.substring(10) + RESET);
                } else if (message.startsWith("SYSTEM:")) {
                    System.out.println(message.substring(7)); // Выводим системные сообщения без префикса
                } else if (message.startsWith("USERS:")) {
                    // Добавляем сообщение со списком пользователей в очередь
                    messageQueue.add(message);
                }
            }
        } catch (IOException e) {
            System.out.println(RED + "Ошибка получения сообщений: " + e.getMessage() + RESET);
        }
    }
}
