package me.chayapak1.chomens_bot.song;

import me.chayapak1.chomens_bot.Bot;
import org.cloudburstmc.math.vector.Vector3d;

import java.nio.charset.StandardCharsets;

public class TextFileConverter implements Converter {
    @Override
    public Song getSongFromBytes (final byte[] bytes, final String fileName, final Bot bot) {
        final String data = new String(bytes, StandardCharsets.UTF_8);

        if (!data.contains(":")) return null;

        int length = 0;

        final Song song = new Song(fileName, bot, null, null, null, null, null, false);

        for (final String line : data.split("\r\n|\r|\n")) {
            if (line.isBlank()) continue;

            // worst way to implement this but it works lol
            if (line.startsWith("title:")) {
                song.songName = line.substring("title:".length());
                continue;
            } else if (line.startsWith("author:")) {
                song.songAuthor = line.substring("author:".length());
                continue;
            } else if (line.startsWith("originalAuthor:")) {
                song.songOriginalAuthor = line.substring("originalAuthor:".length());
                continue;
            } else if (line.startsWith("description:")) {
                song.songDescription = line.substring("description:".length());
                continue;
            }

            song.updateName();

            final String[] split = line.split(":");

            final int tick = Integer.parseInt(split[0]);
            final int pitch = (int) Float.parseFloat(split[1]);
            final String instrument = split[2];

            int intInstrument = -1;
            try {
                intInstrument = Integer.parseInt(instrument);
            } catch (final NumberFormatException ignored) { }

            float volume = 1;
            if (split.length > 3) volume = Float.parseFloat(split[3]);

            final int time = tick * 50;

            length = Math.max(length, time);

            song.add(
                    new Note(
                            intInstrument == -1 ?
                                    Instrument.of(instrument) :
                                    Instrument.fromId(intInstrument),
                            pitch,
                            pitch,
                            volume,
                            time,
                            Vector3d.ZERO,
                            false
                    )
            );
        }

        song.length = song.get(song.size() - 1).time + 50;

        return song;
    }
}
