package org.example;

import java.io.*;

public class UserInterface {

    public static String getNicknameFromUser() throws IOException {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Введите ваш никнейм: ");
        return consoleReader.readLine();
    }

    public static void showMessage(String message) {
        System.out.println(message);
    }
}
