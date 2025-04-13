package me.chayapak1.chomens_bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    public List<String> prefixes;
    public List<String> commandSpyPrefixes;

    public String consoleCommandPrefix;

    public Keys keys = new Keys();
    public Backup backup = new Backup();

    public Database database = new Database();

    public ChomeNSMod chomeNSMod = new ChomeNSMod();

    public String weatherApiKey;

    public String namespace = "chomens_bot";

    public Core core = new Core();
    public Discord discord = new Discord();
    public IRC irc = new IRC();
    public Music music = new Music();

    public Eval eval = new Eval();

    public ColorPalette colorPalette = new ColorPalette();

    public String ownerName = "chayapak"; // mabe mabe

    public String consoleChatFormat = "{\"translate\":\"chat.type.text\",\"with\":[\"OWNER_NAME\",\"MESSAGE\"]}";

    public OwnerAuthentication ownerAuthentication = new OwnerAuthentication();

    public boolean announceClearChatUsername = false;

    public List<String> trusted = new ArrayList<>();
    public SelfCare selfCare = new SelfCare();

    public BotOption[] bots = new BotOption[] {};

    public static class OwnerAuthentication {
        public boolean enabled = false;
        public int timeout = 10 * 1000;
    }

    public static class Backup {
        public boolean enabled = false;
        public String address = "http://fard.sex/check";
        public int interval = 1000;
        public int failTimes = 2;
    }

    public static class Database {
        public boolean enabled = false;
        public String address = "localhost";
        public String username = "chomens_bot";
        public String password = "123456";
    }

    public static class ChomeNSMod {
        public boolean enabled = false;
        public String password = "123456";
        public List<String> players = new ArrayList<>();
    }

    public static class Keys {
        public String trustedKey;
        public String adminKey;
        public String ownerKey;
    }

    public static class Core {
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
        public String defaultColor = "gray";
        public String username = "gold";
        public String uuid = "aqua";
        public String string = "aqua";
        public String number = "gold";
        public String ownerName = "green";
    }

    public static class Discord {
        public boolean enabled = false;
        public String prefix = "default!";
        public String serverId;
        public boolean enableDiscordHashing = true;
        public String token;
        public Map<String, String> servers = new HashMap<>();
        public EmbedColors embedColors = new EmbedColors();
        public String trustedRoleName = "Trusted";
        public String adminRoleName = "Admin";
        public String ownerRoleName = "Owner";
        public String statusMessage = "Oh hi!";
        public String inviteLink = "https://discord.gg/xdgCkUyaA4";
    }

    public static class IRC {
        public boolean enabled = false;
        public String prefix = "!";
        public String host;
        public int port;
        public String name = "chomens-bot";
        public String password = "";
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

    public static class Eval {
        public String address = "ws://localhost:3069";
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
        public String serverName;
        public boolean hidden = false;
        public boolean useCore = true;
        public boolean useCorePlaceBlock = false;
        public boolean useChat = false;
        public boolean coreCommandSpy = true;
        public boolean resolveSRV = true;
        public int reconnectDelay = 2000;
        public int chatQueueDelay = 125;
        public EssentialsMessages essentialsMessages = new EssentialsMessages();
        public CoreRateLimit coreRateLimit = new CoreRateLimit();

        public static class EssentialsMessages {
            public String vanishEnable1 = "Vanish for %s: enabled";
            public String vanishEnable2 = "You are now completely invisible to normal users, and hidden from in-game commands.";
            public String vanishDisable = "Vanish for %s: disabled";

            public String nickNameRemove = "You no longer have a nickname.";
            public String nickNameSet = "Your nickname is now ";

            public String socialSpyEnable = "SocialSpy for %s: enabled";
            public String socialSpyDisable = "SocialSpy for %s: disabled";

            public String muted = "You have been muted";
            public String unmuted = "You have been unmuted.";
        }

        public static class CoreRateLimit {
            public int limit = 0;
            public int reset = 0;
        }
    }
}
