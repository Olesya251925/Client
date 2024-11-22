package org.example;

import java.io.*;
import java.net.*;
import java.util.Properties;

public class ChatClient {

    private static String SERVER_HOST;
    private static int SERVER_PORT;
    private static final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private static PrintWriter out;
    private static BufferedReader in;

    static {
        // Загружаем конфигурацию из файла
        try (InputStream inputStream = ChatClient.class.getClassLoader().getResourceAsStream("client.properties")) {
            Properties properties = new Properties();
            if (inputStream != null) {
                properties.load(inputStream);
                SERVER_HOST = properties.getProperty("server.host", "localhost");
                SERVER_PORT = Integer.parseInt(properties.getProperty("server.port", "12345"));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке конфигурации: " + e.getMessage());
            SERVER_HOST = "localhost";  // Значения по умолчанию
            SERVER_PORT = 12345;
        }
    }

    public static void main(String[] args) throws IOException {
        // Подключаемся к серверу с использованием параметров из конфигурации
        Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Получение никнейма
        System.out.print("Введите ваш никнейм: ");
        String nickname = consoleReader.readLine();
        out.println(nickname);

        // Работа с сообщениями
        String message;
        while (true) {
            System.out.println("1. Личное сообщение");
            System.out.println("2. Широковещательное сообщение");
            System.out.print("Выберите тип сообщения: ");
            int choice = Integer.parseInt(consoleReader.readLine());

            if (choice == 1) {
                System.out.print("Введите никнейм получателя: ");
                String recipient = consoleReader.readLine();
                System.out.print("Введите сообщение: ");
                message = consoleReader.readLine();
                out.println("PRIVATE " + recipient + ": " + message);
            } else if (choice == 2) {
                System.out.print("Введите сообщение для всех: ");
                message = consoleReader.readLine();
                out.println("BROADCAST: " + message);
            }
        }
    }
}
