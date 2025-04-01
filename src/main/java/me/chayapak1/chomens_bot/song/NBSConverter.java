package me.chayapak1.chomens_bot.song;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.LevenshteinUtilities;
import me.chayapak1.chomens_bot.util.StringUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Author: hhhzzzsss
public class NBSConverter implements Converter {
    public static final Instrument[] INSTRUMENT_INDEX = new Instrument[] {
            Instrument.HARP,
            Instrument.BASS,
            Instrument.BASEDRUM,
            Instrument.SNARE,
            Instrument.HAT,
            Instrument.GUITAR,
            Instrument.FLUTE,
            Instrument.BELL,
            Instrument.CHIME,
            Instrument.XYLOPHONE,
            Instrument.IRON_XYLOPHONE,
            Instrument.COW_BELL,
            Instrument.DIDGERIDOO,
            Instrument.BIT,
            Instrument.BANJO,
            Instrument.PLING,
    };

    public static final Map<String, String> CUSTOM_INSTRUMENT_REPLACEMENTS = new HashMap<>();

    static {
        CUSTOM_INSTRUMENT_REPLACEMENTS.put(".*glass.*", "block.glass.break");
        CUSTOM_INSTRUMENT_REPLACEMENTS.put(".*door.*", "block.wooden_door.open");
        CUSTOM_INSTRUMENT_REPLACEMENTS.put(".*anvil.*", "block.anvil.fall");
        CUSTOM_INSTRUMENT_REPLACEMENTS.put(".*piston.*", "block.piston.extend");
        CUSTOM_INSTRUMENT_REPLACEMENTS.put(".*explode|explosion.*", "entity.generic.explode");
        CUSTOM_INSTRUMENT_REPLACEMENTS.put(".*eye.*", "block.end_portal_frame.fill");
        CUSTOM_INSTRUMENT_REPLACEMENTS.put("fizz", "block.fire.extinguish"); // not really sure what this exactly is, but it exists in some NBSes
    }

    public static class NBSNote {
        public int tick;
        public short layer;
        public byte instrument;
        public byte key;
        public byte velocity = 100;
        public byte panning = 100;
        public short pitch = 0;
    }

    public static class NBSLayer {
        public String name;
        public byte lock = 0;
        public byte volume;
        public byte stereo = 100;
    }

    private static class NBSCustomInstrument {
        public String name;
        public String file;
        public byte pitch = 0;
        public boolean key = false;
    }

    @Override
    public Song getSongFromBytes (byte[] bytes, String fileName, Bot bot) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        short songLength;
        byte format = 0;
        byte vanillaInstrumentCount = 0;
        songLength = buffer.getShort(); // If it's not 0, then it uses the old format
        if (songLength == 0) {
            format = buffer.get();
        }

        if (format >= 1) {
            vanillaInstrumentCount = buffer.get();
        }
        if (format >= 3) {
            songLength = buffer.getShort();
        }

        short layerCount = buffer.getShort();
        String songName = getString(buffer, bytes.length);
        String songAuthor = getString(buffer, bytes.length);
        String songOriginalAuthor = getString(buffer, bytes.length);
        String songDescription = getString(buffer, bytes.length);
        double tempo = buffer.getShort();
        byte autoSaving = buffer.get();
        byte autoSavingDuration = buffer.get();
        byte timeSignature = buffer.get();
        int minutesSpent = buffer.getInt();
        int leftClicks = buffer.getInt();
        int rightClicks = buffer.getInt();
        int blocksAdded = buffer.getInt();
        int blocksRemoved = buffer.getInt();
        String origFileName = getString(buffer, bytes.length);

        byte loop = 0;
        byte maxLoopCount = 0;
        short loopStartTick = 0;
        if (format >= 4) {
            loop = buffer.get();
            maxLoopCount = buffer.get();
            loopStartTick = buffer.getShort();
        }

        ArrayList<NBSNote> nbsNotes = new ArrayList<>();
        short tick = -1;
        while (true) {
            int tickJumps = buffer.getShort();
            if (tickJumps == 0) break;
            tick += (short) tickJumps;

            short layer = -1;
            while (true) {
                int layerJumps = buffer.getShort();
                if (layerJumps == 0) break;
                layer += (short) layerJumps;
                NBSNote note = new NBSNote();
                note.tick = tick;
                note.layer = layer;
                note.instrument = buffer.get();
                note.key = buffer.get();
                if (format >= 4) {
                    note.velocity = buffer.get();
                    note.panning = buffer.get();
                    note.pitch = buffer.getShort();
                }
                nbsNotes.add(note);
            }
        }

        ArrayList<NBSLayer> nbsLayers = new ArrayList<>();
        if (buffer.hasRemaining()) {
            for (int i = 0; i < layerCount; i++) {
                NBSLayer layer = new NBSLayer();
                layer.name = getString(buffer, bytes.length);
                if (format >= 4) {
                    layer.lock = buffer.get();
                }
                layer.volume = buffer.get();
                if (format >= 2) {
                    layer.stereo = buffer.get();
                }
                nbsLayers.add(layer);
            }
        }

        ArrayList<NBSCustomInstrument> customInstruments = new ArrayList<>();
        if (buffer.hasRemaining()) {
            byte customInstrumentCount = buffer.get();
            for (int i = 0; i < customInstrumentCount; i++) {
                NBSCustomInstrument customInstrument = new NBSCustomInstrument();
                customInstrument.name = getString(buffer, bytes.length);
                customInstrument.file = getString(buffer, bytes.length);
                customInstrument.pitch = buffer.get();
                customInstrument.key = buffer.get() != 0;
                customInstruments.add(customInstrument);
            }
        }

        final StringBuilder layerNames = new StringBuilder();

        for (NBSLayer layer : nbsLayers) {
            layerNames.append(layer.name);
            layerNames.append("\n");
        }

        final String stringLayerNames = layerNames.toString();

        Song song = new Song(!songName.isBlank() ? songName : fileName, bot, songName, songAuthor, songOriginalAuthor, songDescription, stringLayerNames.substring(0, stringLayerNames.length() - 1), true);
        if (loop > 0) {
            song.loopPosition = getMilliTime(loopStartTick, tempo);
            //      song.loopCount = maxLoopCount;
        }
        for (NBSNote note : nbsNotes) {
            boolean isRainbowToggle = false;
            Instrument instrument;
            double key = note.key;
            if (note.instrument < INSTRUMENT_INDEX.length) {
                instrument = INSTRUMENT_INDEX[note.instrument];

                key = (double) ((note.key * 100) + note.pitch) / 100;
            } else {
                final int index = note.instrument - INSTRUMENT_INDEX.length;

                if (index >= customInstruments.size()) continue;

                final NBSCustomInstrument customInstrument = customInstruments.get(index);

                String name = customInstrument.name
                        .replace("entity.firework.", "entity.firework_rocket."); // this one is special

                boolean isTempoChanger = false;

                if (name.equals("Tempo Changer")) {
                    isTempoChanger = true;

                    tempo = (double) Math.abs(note.pitch) * 100 / 15;
                } else if (name.equals("Toggle Rainbow")) {
                    isRainbowToggle = true;
                }

                String file = Path.of(customInstrument.file).getFileName().toString();

                // should i hardcode the extension like this?
                if (file.endsWith(".ogg")) file = file.substring(0, file.length() - ".ogg".length());

                if (!sounds.contains(name) && !sounds.contains(file) && !isTempoChanger) {
                    boolean replaced = false;

                    final String replacedName = StringUtilities.replaceAllWithMap(name.toLowerCase(), CUSTOM_INSTRUMENT_REPLACEMENTS);
                    final String replacedFile = StringUtilities.replaceAllWithMap(file.toLowerCase(), CUSTOM_INSTRUMENT_REPLACEMENTS);

                    if (!file.equals(replacedFile)) {
                        file = replacedFile;
                        replaced = true;
                    } else if (!name.equals(replacedName)) {
                        name = replacedName;
                        replaced = true;
                    }

                    if (!replaced) {
                        final List<String> outputTitles = LevenshteinUtilities.searchTitles(name, subtitles.values());

                        final String bestMatch = outputTitles.isEmpty() ? "" : outputTitles.getFirst();

                        for (Map.Entry<String, String> entry : subtitles.entrySet()) {
                            if (!entry.getValue().equals(bestMatch)) continue;

                            name = entry.getKey().substring("subtitles.".length());

                            break;
                        }
                    }
                }

                if (!sounds.contains(name) && sounds.contains(file)) name = file;

                instrument = Instrument.of(name);

                key += (double) (customInstrument.pitch + note.pitch) / 100;
            }

            byte layerVolume = 100;
            if (nbsLayers.size() > note.layer) {
                layerVolume = nbsLayers.get(note.layer).volume;
            }

            double pitch = key - 33;

            song.add(
                    new Note(
                            instrument,
                            pitch,
                            key,
                            (float) note.velocity * (float) layerVolume / 10000f,
                            getMilliTime(note.tick, tempo),
                            Byte.toUnsignedInt(note.panning),
                            Byte.toUnsignedInt(nbsLayers.get(note.layer).stereo),
                            isRainbowToggle
                    )
            );
        }

        song.length = song.get(song.size() - 1).time + 50;

        return song;
    }

    private static String getString (ByteBuffer buffer, int maxSize) throws IOException {
        int length = buffer.getInt();
        if (length > maxSize) {
            throw new IOException("String is too large");
        }
        byte[] arr = new byte[length];
        buffer.get(arr, 0, length);
        return new String(arr, StandardCharsets.UTF_8);
    }

    private static long getMilliTime (int tick, double tempo) {
        return (long) (1000L * tick * 100 / tempo);
    }

    private static final Map<String, String> subtitles = new HashMap<>();

    static {
        for (Map.Entry<String, String> entry : ComponentUtilities.LANGUAGE.entrySet()) {
            if (!entry.getKey().startsWith("subtitles.")) continue;

            subtitles.put(entry.getKey(), entry.getValue());
        }
    }

    private static final List<String> sounds = loadJsonStringArray("sounds.json");

    private static List<String> loadJsonStringArray (String name) {
        List<String> list = new ArrayList<>();

        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        JsonArray json = JsonParser.parseReader(reader).getAsJsonArray();

        for (JsonElement entry : json) {
            list.add(entry.getAsString());
        }

        return list;
    }
}
