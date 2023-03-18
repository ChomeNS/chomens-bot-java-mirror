package me.chayapak1.chomensbot_mabe;

public class Main {
    public static void main(String[] args) {
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String username = args[2];

        new Bot(host, port, username);
    }
}