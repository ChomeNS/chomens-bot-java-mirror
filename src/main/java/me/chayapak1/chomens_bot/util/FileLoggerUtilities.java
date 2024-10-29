package me.chayapak1.chomens_bot.util;

import me.chayapak1.chomens_bot.Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

// totallynotskidded™ from HBot
public class FileLoggerUtilities {
    public static final Path logDirectory = Path.of("logs");
    public static final Path logPath = Paths.get(logDirectory.toString(), "log.txt");

    public static BufferedWriter logWriter;

    public static LocalDate currentLogDate;
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("'['dd/MM/yyyy HH:mm:ss']' ");

    public static String prevEntry = "";
    public static int duplicateCounter = 1;

    public static final ScheduledExecutorService executor = Main.executor;
    public static int spamLevel = 0;
    public static long freezeTime = 0;

    static {
        init();
    }

    public static void init() {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void tick() {
        if (freezeTime <= System.currentTimeMillis() && spamLevel > 0) {
            spamLevel--;
        }

        if (!currentLogDate.equals(LocalDate.now())) {
            try {
                compressLogFile();
                makeNewLogFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void makeNewLogFile() throws IOException {
        currentLogDate = LocalDate.now();

        if (!Files.exists(logPath)) Files.createFile(logPath);

        logWriter = Files.newBufferedWriter(logPath, StandardOpenOption.TRUNCATE_EXISTING);
        logWriter.write(currentLogDate.toString() + '\n');
        logWriter.flush();
    }

    public static void openLogFile() throws IOException {
        currentLogDate = LocalDate.parse(getLogDate(logPath));
        logWriter = Files.newBufferedWriter(logPath, StandardCharsets.ISO_8859_1, StandardOpenOption.APPEND);
    }

    public static void compressLogFile() throws IOException {
        if (Files.size(logPath) > 100 * 1024 * 1024) { // Will not save because log file is too big
            return;
        }

        final Path path = Paths.get(logDirectory.toString(), getLogDate(logPath) + ".txt.gz");

        Files.createFile(path);

        InputStream in = Files.newInputStream(logPath);
        GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(path));

        byte[] buffer = new byte[1024];
        int size;
        while ((size = in.read(buffer)) > 0) {
            out.write(buffer, 0, size);
        }
        in.close();
        out.finish();
        out.close();
    }

    public static String getLogDate(Path path) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path);
        String date = reader.readLine();
        reader.close();
        return date;
    }

    public static boolean logIsCurrent(Path path) throws IOException {
        LocalDate date = LocalDate.now();
        return getLogDate(path).equals(date.toString());
    }

    public static void log(String str) {
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

                if (str.length() > 32767) logWriter.write("Message too big, not logging this message"); // should these stuff be hardcoded?
                else logWriter.write(getTimePrefix() + str.replaceAll("\\[(\\d+?)x](?=$|[\r\n])", "[/$1x]")); // the replaceAll will prevent conflicts with the duplicate counter
                logWriter.flush();

                duplicateCounter = 1;
                prevEntry = str;

                spamLevel += 2;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getTimePrefix() {
        LocalDateTime dateTime = LocalDateTime.now();
        return dateTime.format(dateTimeFormatter);
    }
}
