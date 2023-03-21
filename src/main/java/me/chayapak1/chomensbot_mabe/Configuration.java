package me.chayapak1.chomensbot_mabe;

import lombok.Getter;

import java.util.Map;

public class Configuration {
    @Getter public int reconnectDelay = 7000;
    @Getter public Map<String, String> keys;
    @Getter public Bots[] bots = new Bots[]{};

    public static class Bots {
        @Getter public String host;
        @Getter public int port;
        @Getter public String username;
    }
}
