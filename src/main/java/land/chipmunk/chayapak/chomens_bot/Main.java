package land.chipmunk.chayapak.chomens_bot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import land.chipmunk.chayapak.chomens_bot.plugins.ConsolePlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final List<Bot> bots = new ArrayList<>();

    public static final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("ExecutorService #%d").build()
    );
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("ScheduledExecutorService #%d").build()
    );

    private static Configuration config;

    public static void main(String[] args) throws IOException {
        final File file = new File("config.yml");
        final Constructor constructor = new Constructor(Configuration.class, new LoaderOptions());
        final Yaml yaml = new Yaml(constructor);

        if (!file.exists()) {
            // creates config file from default-config.yml
            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.yml");

            if (is == null) System.exit(1);

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
        }

        InputStream opt = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        config = yaml.load(reader);

        executor.scheduleAtFixedRate(() -> {
            try {
                checkInternet();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);

        Configuration.BotOption[] botsOptions = config.bots;

        // idk if these should be here lol, but it is just the discord stuff
        JDA jda = null;
        if (config.discord.enabled) {
            JDABuilder builder = JDABuilder.createDefault(config.discord.token);
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
            jda.getPresence().setPresence(Activity.playing(config.discord.statusMessage), false);
        }

        for (Configuration.BotOption botOption : botsOptions) {
            final Bot bot = new Bot(botOption, bots, config);
            bots.add(bot);
        }

        // fard
        new ConsolePlugin(bots, config, jda);
    }

    private static void checkInternet () throws IOException {
        if (!config.internetCheck.enabled) return;

        boolean reachable = false;

        try {
            final URL url = new URL(config.internetCheck.address);

            final URLConnection connection = url.openConnection();

            connection.connect();

            reachable = true;
        } catch (UnknownHostException ignored) {}

        if (!reachable) {
            System.err.println("No internet, exiting");

            System.exit(1);
        }
    }
}