package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Logger;
import lombok.Getter;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class GrepLogPlugin {
    private final Bot bot;

    @Getter private GrepLogThread thread = null;

    public GrepLogPlugin (Bot bot) {
        this.bot = bot;
    }

    public void query (String query, boolean regex, boolean ignoreCase) {
        thread = new GrepLogThread(query, regex, ignoreCase);
        thread.start();
    }

    // should i move this to another file or keep it here
    public class GrepLogThread extends Thread {
        private String query;
        private final boolean regex;
        private final boolean ignoreCase;

        private Pattern pattern;

        private boolean queryStopped = false;

        private int matches = 0;
        private final StringBuilder results = new StringBuilder();

        public GrepLogThread(String query, boolean regex, boolean ignoreCase) {
            this.regex = regex;
            this.ignoreCase = ignoreCase;

            this.query = query;

            if (regex) {
                if (ignoreCase) {
                    pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
                }
                else {
                    pattern = Pattern.compile(query);
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
                            Component.text(query).color(NamedTextColor.GOLD)
                    )
            );

            final File[] fileList = Logger.logDir.listFiles();

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
                    } catch (Exception ignored) {
                    } // TODO: Handle exception
                } else {
                    try (
                            FileInputStream fin = new FileInputStream(file);
                            BufferedReader br = new BufferedReader(new InputStreamReader(fin, StandardCharsets.UTF_8))
                    ) {
                        br.readLine();
                        readFile(br);
                    } catch (Exception ignored) {
                    } // TODO: Handle exception
                }

                if (Thread.interrupted()) {
                    bot.chat().tellraw(Component.text("Log query stopped"));
                } else if (queryStopped) {
                    break;
                }
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
            bot.chat().tellraw(
                    Component.translatable(
                            "Log query finished, found %s matches. Results were sent in Discord",
                            Component.text(matches).color(NamedTextColor.AQUA)
                    )
            );

            final String channelId = bot.discord().servers.get(bot.host() + ":" + bot.port());
            final TextChannel logChannel = bot.discord().jda().getTextChannelById(channelId);
            logChannel.sendMessage("Log query output").addFile(results.toString().getBytes(), "report.txt").queue();
        }
    }
}
