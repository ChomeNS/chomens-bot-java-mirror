package land.chipmunk.chayapak.chomens_bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    public List<String> prefixes;
    public List<String> commandSpyPrefixes;

    public String consoleCommandPrefix;

    public Keys keys = new Keys();

    public InternetCheck internetCheck = new InternetCheck();

    public Backup backup = new Backup();

    public String weatherApiKey;

    public String bossBarNamespace = "chomens_bot";

    public Core core = new Core();
    public Discord discord = new Discord();
    public IRC irc = new IRC();
    public Music music = new Music();

    public ColorPalette colorPalette = new ColorPalette();
    
    public String ownerName = "chayapak"; // mabe mabe

    public ImposterFormatChecker imposterFormatChecker = new ImposterFormatChecker();

    public OwnerAuthentication ownerAuthentication = new OwnerAuthentication();
    
    public List<String> trusted = new ArrayList<>();
    public SelfCare selfCare = new SelfCare();

    public BotOption[] bots = new BotOption[]{};

    public static class ImposterFormatChecker {
        public boolean enabled = false;
        public String key;
    }

    public static class OwnerAuthentication {
        public boolean enabled = false;
        public String key = "";
        public int timeout = 6000;
    }

    public static class InternetCheck {
        public boolean enabled = true;
        public String address = "https://sus.red";
    }

    public static class Backup {
        public boolean enabled = false;
        public String address = "http://fard.sex/check";
    }

    public static class Keys {
        public String normalKey;
        public String ownerKey;
    }

    public static class Core {
        public Position start = new Position();
        public Position end = new Position();
        public int refillInterval = (60 * 5) * 1000; // 5 minutes
        public String customName = "{\"text\":\"@\"}";
    }

    public static class Position {
        public int x = 0;
        public int y = 0;
        public int z = 0;
    }

    public static class ColorPalette {
        public String primary = "yellow";
        public String secondary = "gold";
        public String defaultColor = "white";
        public String username = "gold";
        public String uuid = "aqua";
        public String string = "aqua";
        public String number = "gold";
        public String ownerName = "green";
    }

    public static class Discord {
        public boolean enabled = true;
        public String prefix = "default!";
        public String token;
        public Map<String, String> servers = new HashMap<>();
        public EmbedColors embedColors = new EmbedColors();
        public String trustedRoleName = "Trusted";
        public String adminRoleName = "Admin";
        public String statusMessage = "Gay Sex";
        public String inviteLink = "https://discord.gg/xdgCkUyaA4";
    }

    public static class IRC {
        public boolean enabled = true;
        public String prefix = "!";
        public String host;
        public int port;
        public String nickname = "chomens-bot";
        public String username = "chomens-bot";
        public String realName = "chomens-bot";
        public String hostName = "null";
        public String serverName = "null";
        public Map<String, String> servers = new HashMap<>();
    }

    public static class Music {
        public URLRatelimit urlRatelimit = new URLRatelimit();

        public static class URLRatelimit {
            public int seconds = 15;
            public int limit = 7;
        }
    }

    public static class EmbedColors {
        public String normal = "#FFFF00";
        public String error = "#FF0000";
    }

    public static class SelfCare {
        public int delay = 225;

        public boolean op = true;
        public boolean gamemode = true;
        public boolean endCredits = true;

        public boolean vanish = true;
        public boolean nickname = true;
        public boolean socialspy = true;
        public boolean mute = true;

        public boolean cspy = true;

        public Icu icu = new Icu();

        public static class Icu {
            public boolean enabled = true;
            public int positionPacketsPerSecond = 10;
        }

        public Prefix prefix = new Prefix();

        public static class Prefix {
            public boolean enabled = true;
            public String prefix = "&8[&eChomeNS Bot&8]";
        }

        public boolean username = true;
    }

    public static class BotOption {
        public String host;
        public int port;
        public String username;
        public boolean creayun = false;
        public String serverName;
        public boolean useCore = true;
        public boolean useChat = false;
        public int reconnectDelay = 2000;
        public boolean removeNamespaces = false;
        public int chatQueueDelay = 125;
        public CoreRateLimit coreRateLimit = new CoreRateLimit();

        public static class CoreRateLimit {
            public int limit = 0;
            public int reset = 0;
        }
    }
}
