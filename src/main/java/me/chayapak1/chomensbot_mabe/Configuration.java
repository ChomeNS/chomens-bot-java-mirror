package me.chayapak1.chomensbot_mabe;

import lombok.Getter;

import java.util.Map;

public class Configuration {
    @Getter public int reconnectDelay = 7000;
    @Getter public Map<String, String> keys;
    @Getter public Core core = new Core();
    @Getter public SelfCare selfCare = new SelfCare();
    @Getter public Bots[] bots = new Bots[]{};

    public static class Core {
        @Getter public int layers = 3;
        @Getter public int refillInterval = 60 * 1000;
        @Getter public String customName = "[{\"text\":\"ChomeNS \",\"color\":\"yellow\"},{\"text\":\"Core\",\"color\":\"green\"},{\"text\":\"™\",\"color\":\"gold\"}]";
    }

    public static class SelfCare {
        @Getter public int checkInterval;

        @Getter public boolean op = true;
        @Getter public boolean gamemode = true;
        @Getter public boolean endCredits = true;

        @Getter public boolean cspy = true;
        @Getter public boolean vanish = true;
        @Getter public boolean nickname = true;
        @Getter public boolean socialspy = true;
        @Getter public boolean mute = true;

        @Getter public boolean prefix = true;
    }

    public static class Bots {
        @Getter public String host;
        @Getter public int port;
        @Getter public String username;
    }
}
