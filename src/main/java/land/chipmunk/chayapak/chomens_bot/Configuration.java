package land.chipmunk.chayapak.chomens_bot;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    @Getter public List<String> prefixes;
    @Getter public List<String> commandSpyPrefixes;

    @Getter public Map<String, String> consolePrefixes;

    @Getter public int reconnectDelay = 2000;

    @Getter public int chatQueueDelay = 125;

    @Getter public Map<String, String> keys;

    @Getter public String weatherApiKey;

    @Getter public Core core = new Core();
    @Getter public Discord discord = new Discord();
    
    @Getter public String ownerName = "chayapak"; // mabe mabe
    
    @Getter public List<String> trusted = new ArrayList<>();
    @Getter public SelfCare selfCare = new SelfCare();
    @Getter public BotOption[] bots = new BotOption[]{};

    public static class Core {
        @Getter public int layers = 3;
        @Getter public int refillInterval = (60 * 5) * 1000; // 5 minutes
        @Getter public String customName = "[{\"text\":\"ChomeNS \",\"color\":\"yellow\"},{\"text\":\"Core\",\"color\":\"green\"},{\"text\":\"™\",\"color\":\"gold\"}]";
    }

    public static class Discord {
        @Getter public String prefix = "default!";
        @Getter public String token;
        @Getter public Map<String, String> servers = new HashMap<>();
        @Getter public EmbedColors embedColors = new EmbedColors();
        @Getter public String trustedRoleName = "Trusted";
        @Getter public String adminRoleName = "Admin";
        @Getter public String statusMessage = "Gay Sex";
        @Getter public String inviteLink = "https://discord.gg/xdgCkUyaA4";
    }

    public static class EmbedColors {
        @Getter public String normal = "#FFFF00";
        @Getter public String error = "#FF0000";
    }

    public static class SelfCare {
        @Getter public int checkInterval;

        @Getter public boolean op = true;
        @Getter public boolean gamemode = true;
        @Getter public boolean endCredits = true;

        @Getter public boolean vanish = true;
        @Getter public boolean nickname = true;
        @Getter public boolean socialspy = true;
        @Getter public boolean mute = true;

        @Getter public boolean cspy = true;

        @Getter public AntiIcuSelfCare icu = new AntiIcuSelfCare();

        @Getter public boolean prefix = true;
        @Getter public boolean username = true;
    }

    public static class AntiIcuSelfCare {
        @Getter public boolean enabled = true;
        @Getter public int positionPacketsPerSecond = 10;
    }

    public static class BotOption {
        @Getter public String host;
        @Getter public int port;
        @Getter public String username;
        @Getter public boolean kaboom = false;
        @Getter public String serverName;
        @Getter @Setter public boolean useCore = true;
        @Getter @Setter public boolean useChat = false;
        @Getter public boolean hasEssentials = true;
        @Getter public int reconnectDelay = -1;
    }
}
