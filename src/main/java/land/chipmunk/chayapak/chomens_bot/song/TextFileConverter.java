package land.chipmunk.chayapak.chomens_bot.song;

import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.Arrays;

public class TextFileConverter implements Converter {
    @Override
    public Song getSongFromBytes(byte[] bytes, String fileName, Bot bot) {
        final String data = new String(bytes);

        if (!data.contains(":")) return null;

        int length = 0;

        final Song song = new Song(fileName, bot, null, null, null, null, false);

        for (String line : data.split("\r\n|\r|\n")) {
            if (line.isEmpty()) continue;

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


            final Integer[] mapped = Arrays.stream(line.split(":")).map(Integer::parseInt).toArray(Integer[]::new);

            final int tick = mapped[0];
            final int pitch = mapped[1];
            final int instrument = mapped[2];

            final int time = tick * 50;

            length = Math.max(length, time);

            song.add(new Note(Instrument.fromId(instrument), pitch, 1, time, -1, 100));
        }

        song.length = song.get(song.size() - 1).time + 50;

        return song;
    }
}
