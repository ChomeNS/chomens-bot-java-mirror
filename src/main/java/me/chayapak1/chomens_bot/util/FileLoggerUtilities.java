package me.chayapak1.chomens_bot.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.chayapak1.chomens_bot.data.logging.LogType;
import net.kyori.adventure.text.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

// original source code from hhhzzzsss, specifically HBot.
// source: https://github.com/hhhzzzsss/HBot-Release/blob/main/src/main/java/com/github/hhhzzzsss/hbot/Logger.java
public class FileLoggerUtilities {
    public static final Path logDirectory = Path.of("logs");
    public static final Path logPath = Paths.get(logDirectory.toString(), "log.txt");

    public static OutputStreamWriter logWriter;

    public static LocalDate currentLogDate;
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static String prevEntry = "";
    public static int duplicateCounter = 1;

    // we want a completely separate executor from the main one
    public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("ScheduledExecutorService (logger)")
                    .build()
    );

    public static int spamLevel = 0;
    public static long freezeTime = 0;

    static {
        init();
    }

    public static void init () {
        try {
            if (!Files.exists(logDirectory)) Files.createDirectory(logDirectory);

            if (!Files.exists(logPath)) {
                makeNewLogFile();
            } else if (!logIsCurrent(logPath)) {
                compressLogFile();
                makeNewLogFile();
            } else {
                openLogFile();
            }

            executor.scheduleAtFixedRate(() -> {
                try {
                    tick();
                } catch (final Exception e) {
                    LoggerUtilities.error(e);
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        } catch (final IOException e) {
            LoggerUtilities.error(e);
        }
    }

    public static void stop () {
        executor.shutdown();
    }

    private static void tick () {
        if (freezeTime <= System.currentTimeMillis() && spamLevel > 0) {
            spamLevel--;
        }

        if (!currentLogDate.equals(LocalDate.now())) {
            try {
                compressLogFile();
                makeNewLogFile();
            } catch (final IOException e) {
                LoggerUtilities.error(e);
            }
        }
    }

    public static synchronized void makeNewLogFile () throws IOException {
        currentLogDate = LocalDate.now();
        logWriter = new OutputStreamWriter(Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8);
        logWriter.write(currentLogDate.toString() + '\n');
        logWriter.flush();
    }

    public static synchronized void openLogFile () throws IOException {
        currentLogDate = LocalDate.parse(getLogDate(logPath));
        logWriter = new OutputStreamWriter(Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND), StandardCharsets.UTF_8);
    }

    public static synchronized void compressLogFile () throws IOException {
        if (Files.size(logPath) > 500 * 1024 * 1024) { // Will not save because log file is too big
            LoggerUtilities.log(
                    LogType.INFO,
                    null,
                    Component.text("Not archiving log file since it's too big!"),
                    false,
                    true
            );
            return;
        }

        final Path path = Paths.get(logDirectory.toString(), getLogDate(logPath) + ".txt.gz");

        try (
                final InputStream in = Files.newInputStream(logPath, StandardOpenOption.READ);
                final GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE))
        ) {
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = in.read(buffer)) > 0) {
                out.write(buffer, 0, size);
            }
        }
    }

    public static synchronized String getLogDate (final Path filePath) throws IOException {
        try (final BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return reader.readLine();
        }
    }

    public static synchronized boolean logIsCurrent (final Path path) throws IOException {
        final LocalDate date = LocalDate.now();
        return getLogDate(path).equals(date.toString());
    }

    public static synchronized void log (final String type, final String str) {
        if (freezeTime > System.currentTimeMillis()) {
            return;
        }

        try {
            if (spamLevel >= 100) {
                spamLevel = 80;
                freezeTime = System.currentTimeMillis() + 20000;

                if (duplicateCounter > 1) {
                    logWriter.write(String.format(" [%sx]\n", duplicateCounter));
                } else {
                    logWriter.write("\n");
                }

                logWriter.write("Spam detected, logs temporarily frozen");
                logWriter.flush();

                duplicateCounter = 1;
                prevEntry = "";

                return;
            }

            if (str.equalsIgnoreCase(prevEntry)) {
                duplicateCounter++;
            } else {
                if (duplicateCounter > 1) {
                    logWriter.write(String.format(" [%sx]\n", duplicateCounter));
                } else {
                    logWriter.write("\n");
                }

                logWriter.write(getPrefix(type) + str.replaceAll("\\[(\\d+?)x](?=$|[\r\n])", "[/$1x]")); // the replaceAll will prevent conflicts with the duplicate counter
                logWriter.flush();

                duplicateCounter = 1;
                prevEntry = str;

                spamLevel += 2;
            }
        } catch (final IOException e) {
            LoggerUtilities.error(e);
        }
    }

    public static String getPrefix (final String type) {
        final LocalDateTime dateTime = LocalDateTime.now();
        return String.format(
                "[%s %s] ",
                dateTime.format(dateTimeFormatter),
                type
        );
    }
}
