package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.LoggerUtilities;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class GrepLogPlugin {
    private final Bot bot;

    @Getter @Setter private Future<?> future = null;

    public GrepLogPlugin (Bot bot) {
        this.bot = bot;
    }

    public void query (String query, boolean regex, boolean ignoreCase) {
        final Runnable runnable = new GrepLogRunnable(query, regex, ignoreCase);
        future = bot.executorService().submit(runnable);
    }

    // should i move this to another file or keep it here
    public class GrepLogRunnable implements Runnable {
        private String query;
        private final boolean regex;
        private final boolean ignoreCase;

        private Pattern pattern;

        private boolean queryStopped = false;

        private int matches = 0;
        private final StringBuilder results = new StringBuilder();

        public GrepLogRunnable(String query, boolean regex, boolean ignoreCase) {
            this.regex = regex;
            this.ignoreCase = ignoreCase;

            this.query = query;

            if (regex) {
                if (ignoreCase) {
                    pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
                }
                else {
                    try {
                        pattern = Pattern.compile(query);
                    } catch (Exception e) {
                        bot.chat().tellraw(Component.text(e.toString()).color(NamedTextColor.RED));
                    }
                }
            } else {
                if (ignoreCase) {
                    this.query = query.toLowerCase();
                }
            }
        }

        @Override
        public void run() {
            bot.chat().tellraw(
                    Component.translatable(
                            "Collecting %s in logs...",
                            Component.text(query).color(ColorUtilities.getColorByString(bot.config().colorPalette().secondary()))
                    ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
            );

            final File[] fileList = LoggerUtilities.logDir.listFiles();

            Arrays.sort(fileList, Comparator.comparing(File::getName)); // VERY IMPORTANT

            for (File file : fileList) {
                final String fileName = file.getName();
                if (fileName.matches(".*\\.txt\\.gz")) {
                    try (
                            FileInputStream fin = new FileInputStream(file);
                            GZIPInputStream gzin = new GZIPInputStream(fin, 65536);
                            BufferedReader br = new BufferedReader(new InputStreamReader(gzin, StandardCharsets.UTF_8))
                    ) {
                        br.readLine();
                        readFile(br);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try (
                            FileInputStream fin = new FileInputStream(file);
                            BufferedReader br = new BufferedReader(new InputStreamReader(fin, StandardCharsets.UTF_8))
                    ) {
                        br.readLine();
                        readFile(br);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (queryStopped) break;
            }

            finish();
        }

        private void readFile(BufferedReader br) {
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    if (line.length() == 0) continue;

                    String[] tokens = line.split("\\s+");
                    if (tokens.length < 3) continue; // not a valid log message

                    processLine(line);
                }
            } catch (IOException ignored) {}
        }

        private void processLine(String _line) {
            // [day/month/year hh:mm:ss] [server:port] message
            String[] tokens = _line.split("\\s+");
            final String line = String.join(" ", Arrays.copyOfRange(tokens, 3, tokens.length)); // very 1 liner

            if (regex) {
                if (pattern.matcher(line).find()) {
                    match(_line);
                }
            } else {
                if (ignoreCase) {
                    if (line.toLowerCase().contains(query)) {
                        match(_line);
                    }
                } else {
                    if (line.contains(query)) {
                        match(_line);
                    }
                }
            }
        }

        private void match (String line) {
            results.append(line);
            results.append("\n");

            matches++;

            int resultsLimit = 1_000_000;
            if (results.length() > resultsLimit) {
                queryStopped = true;
            }
        }

        private void finish () {
            if (results.toString().split("\n").length < 100) { // ig lazy fix for removing \n lol
                bot.chat().tellraw(
                        Component.empty()
                                .append(Component.text("Log query output for \""))
                                .append(Component.text(query))
                                .append(Component.text("\":"))
                                .append(Component.newline())
                                .append(Component.text(results.toString()))
                );
            } else if (bot.config().discord().enabled()) {
                bot.chat().tellraw(
                        Component.translatable(
                                "Log query for \"%s\" finished, found %s matches. Results were sent in Discord",
                                Component.text(query).color(ColorUtilities.getColorByString(bot.config().colorPalette().string())),
                                Component.text(matches).color(ColorUtilities.getColorByString(bot.config().colorPalette().number()))
                        )
                );

                final String channelId = bot.discord().servers.get(bot.host() + ":" + bot.port());
                final TextChannel logChannel = bot.discord().jda().getTextChannelById(channelId);
                logChannel.sendMessage("Log query output").addFile(results.toString().getBytes(), "report.txt").queue();
            }
        }
    }
}
