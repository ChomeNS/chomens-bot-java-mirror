package land.chipmunk.chayapak.chomens_bot;

import land.chipmunk.chayapak.chomens_bot.plugins.ConsolePlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static final List<Bot> allBots = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        final File file = new File("config.yml");
        final Constructor constructor = new Constructor(Configuration.class);
        final Yaml yaml = new Yaml(constructor);
        Configuration _config;

        if (!file.exists()) {
            // creates config file from default-config.yml
            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.yml");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            while (reader.ready()) {
                char character = (char) reader.read();
                stringBuilder.append(character);
            }
            String defaultConfig = stringBuilder.toString();

            // writes it
            BufferedWriter configWriter = new BufferedWriter(new FileWriter(file));
            configWriter.write(defaultConfig);
            configWriter.close();

            System.out.println("config.yml file not found, so the default one was created");

            _config = yaml.load(is);
        }

        InputStream opt = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        _config = yaml.load(reader);

        final Configuration config = _config;

        Configuration.Bots[] botsOptions = config.bots();

        // idk if these should be here lol, but it is just the discord stuff
        JDA jda = null;
        JDABuilder builder = JDABuilder.createDefault(config.discord().token());
        try {
            jda = builder.build();
            jda.awaitReady();
        } catch (LoginException e) {
            System.err.println("Failed to login to Discord, stacktrace:");
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException ignored) {
            System.exit(1);
        }

        for (Configuration.Bots botOption : botsOptions) {
            final String host = botOption.host();
            final int port = botOption.port();
            final String username = botOption.username();
            final boolean kaboom = botOption.kaboom();
            final String serverName = botOption.serverName();

            final Bot bot = new Bot(host, port, username, kaboom, serverName, allBots, config);
            allBots.add(bot);
        }

        new ConsolePlugin(allBots, config, jda);
    }
}