package org.example;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.*;

public class ChatClient {
    public static String SERVER_HOST;
    public static int SERVER_PORT;
    public static BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    public static PrintWriter out;
    public static BufferedReader in;
    public static String nickname;
    public static BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public static final String RESET = "\033[0m";  // Сброс цвета
    public static final String RED = "\033[31m";    // Красный
    public static final String GREEN = "\033[32m";  // Зеленый
    public static final String YELLOW = "\033[33m"; // Желтый
    public static final String CYAN = "\033[36m";   // Циан

    static {
        // Загружаем параметры из файла client.properties
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

            // Создаем NetworkHandler и UserInteraction
            NetworkHandler networkHandler = new NetworkHandler(socket);
            UserInteraction userInteraction = new UserInteraction(networkHandler);
            userInteraction.start();

            // Чтение ника пользователя
            System.out.print("Введите ваш никнейм: ");
            nickname = consoleReader.readLine();
            networkHandler.setNickname(nickname);
            // Отправка ника на сервер
            out.println("SET_NICKNAME:" + nickname);

        } catch (IOException e) {
            System.out.println(RED + "Ошибка подключения: " + e.getMessage() + RESET);
        }
    }
}
