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
    private static boolean userListReceived = false;  // флаг для предотвращения дублирования

    static {
        // Загружаем параметры из файла config.properties
        try (InputStream input = ChatClient.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (input == null) {
                System.out.println("Не удалось найти файл client.properties");


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
            System.out.print("Введите ваш никнейм: ");
            nickname = consoleReader.readLine();
            out.println(nickname);

            System.out.println("Вы подключились к сети!");

            // Создаем поток для получения сообщений от сервера
            new Thread(ChatClient::receiveMessages).start();

            // Ждем системное сообщение о подключении нового пользователя
            String initialMessage = messageQueue.take();
            System.out.println(initialMessage);

            // Теперь сразу показываем меню
            displayMenu();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void displayMenu() throws IOException {
        while (true) {
            System.out.println("\nВыберите действие:");
            System.out.println("    1. Личное сообщение");
            System.out.println("    2. Сообщение для всех");
            System.out.print("Ваш выбор: ");
            String choice = consoleReader.readLine();

            try {
                switch (choice) {
                    case "1":
                        // Очищаем предыдущий список пользователей
                        userListReceived = false;
                        // Запрашиваем новый список
                        getUserList();
                        // Ждем немного, чтобы получить ответ от сервера
                        Thread.sleep(100);
                        // Обрабатываем ответ только один раз
                        processQueuedMessages();
                        sendPrivateMessage();
                        break;
                    case "2":
                        sendBroadcastMessage();
                        break;
                    default:
                        System.out.println("Неверный выбор. Попробуйте снова.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

        private static void sendPrivateMessage() throws IOException {
            System.out.print("Введите никнейм получателя: ");
            String recipient = consoleReader.readLine();
            System.out.print("Введите сообщение: ");
            String message = consoleReader.readLine();
            out.println("PRIVATE:" + recipient + ":" + message);

            // Добавляем логирование отправки личного сообщения
            System.out.println("\nОтправлено личное сообщение:");
            System.out.println("От: " + nickname + " (вы)");
            System.out.println("Кому: " + recipient);
            System.out.println("Сообщение: " + message);
        }

    private static void sendBroadcastMessage() throws IOException {
        System.out.print("Введите сообщение для всех: ");
        String message = consoleReader.readLine();
        out.println("BROADCAST:" + message);

        // Добавляем логирование отправки общего сообщения
        System.out.println("\nОтправлено сообщение всем:");
        System.out.println("От: " + nickname + " (вы)");
        System.out.println("Сообщение: " + message);
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
                    // Показываем список пользователей только один раз
                    if (!userListReceived && parts.length > 1) {
                        displayUserList(parts[1]);
                        userListReceived = true;
                    }
                    break;
                case "PRIVATE":
                    String sender = parts[1];
                    String displaySender = sender.equals(nickname) ? sender + " (вы)" : sender;
                    System.out.println("\nЛичное сообщение:");
                    System.out.println("От: " + displaySender);
                    System.out.println("Сообщение: " + parts[2]);
                    break;
                case "BROADCAST":
                    sender = parts[1];
                    displaySender = sender.equals(nickname) ? sender + " (вы)" : sender;
                    System.out.println("\nСообщение всем:");
                    System.out.println("От: " + displaySender);
                    System.out.println("Сообщение: " + parts[2]);
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
            // Добавляем "(вы)" к имени текущего пользователя
            String displayName = user.equals(nickname) ? user + " (вы)" : user;
            System.out.println("    - " + displayName);
        }
    }
}

