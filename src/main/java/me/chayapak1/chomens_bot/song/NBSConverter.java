package me.chayapak1.chomens_bot.song;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.StringUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

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
        public long tick;
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
    public Song getSongFromBytes (final byte[] bytes, final String fileName, final Bot bot) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
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

        final short layerCount = buffer.getShort();
        final String songName = getString(buffer, bytes.length);
        final String songAuthor = getString(buffer, bytes.length);
        final String songOriginalAuthor = getString(buffer, bytes.length);
        final String songDescription = getString(buffer, bytes.length);
        double tempo = buffer.getShort();
        final byte autoSaving = buffer.get();
        final byte autoSavingDuration = buffer.get();
        final byte timeSignature = buffer.get();
        final int minutesSpent = buffer.getInt();
        final int leftClicks = buffer.getInt();
        final int rightClicks = buffer.getInt();
        final int blocksAdded = buffer.getInt();
        final int blocksRemoved = buffer.getInt();
        final String origFileName = getString(buffer, bytes.length);

        byte loop = 0;
        byte maxLoopCount = 0;
        short loopStartTick = 0;
        if (format >= 4) {
            loop = buffer.get();
            maxLoopCount = buffer.get();
            loopStartTick = buffer.getShort();
        }

        final ArrayList<NBSNote> nbsNotes = new ArrayList<>();
        long tick = -1;
        while (true) {
            final short tickJumps = buffer.getShort();
            if (tickJumps == 0) break;
            tick += tickJumps;

            short layer = -1;
            while (true) {
                final int layerJumps = buffer.getShort();
                if (layerJumps == 0) break;
                layer += (short) layerJumps;
                final NBSNote note = new NBSNote();
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

        final ArrayList<NBSLayer> nbsLayers = new ArrayList<>();
        if (buffer.hasRemaining()) {
            for (int i = 0; i < layerCount; i++) {
                final NBSLayer layer = new NBSLayer();
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

        final ArrayList<NBSCustomInstrument> customInstruments = new ArrayList<>();
        if (buffer.hasRemaining()) {
            final byte customInstrumentCount = buffer.get();
            for (int i = 0; i < customInstrumentCount; i++) {
                final NBSCustomInstrument customInstrument = new NBSCustomInstrument();
                customInstrument.name = getString(buffer, bytes.length);
                customInstrument.file = getString(buffer, bytes.length);
                customInstrument.pitch = buffer.get();
                customInstrument.key = buffer.get() != 0;
                customInstruments.add(customInstrument);
            }
        }

        final StringBuilder layerNames = new StringBuilder();

        for (final NBSLayer layer : nbsLayers) {
            layerNames.append(layer.name);
            layerNames.append("\n");
        }

        final String stringLayerNames = layerNames.toString();

        final Song song = new Song(
                !songName.isBlank() ? songName : fileName,
                bot,
                songName,
                songAuthor,
                songOriginalAuthor,
                songDescription,
                stringLayerNames.substring(0, Math.max(0, stringLayerNames.length() - 1)),
                true
        );
        if (loop > 0) {
            song.loopPosition = getMilliTime(loopStartTick, tempo);
            //      song.loopCount = maxLoopCount;
        }
        for (final NBSNote note : nbsNotes) {
            boolean isRainbowToggle = false;
            final Instrument instrument;
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

                    // causes issues :(
                    // (more specifically empty gaps after the tempo has been changed)
                    // tempo = (double) Math.abs(note.pitch) * 100 / 15;
                } else if (name.equals("Toggle Rainbow")) {
                    isRainbowToggle = true;
                }

                String file = Path.of(customInstrument.file).getFileName().toString();

                // should i hardcode the extension like this?
                if (file.endsWith(".ogg")) file = file.substring(0, file.length() - ".ogg".length());

                if (!sounds.contains(name) && !sounds.contains(file) && !isTempoChanger) {
                    final String replacedName = StringUtilities.replaceAllWithMap(name.toLowerCase(), CUSTOM_INSTRUMENT_REPLACEMENTS);
                    final String replacedFile = StringUtilities.replaceAllWithMap(file.toLowerCase(), CUSTOM_INSTRUMENT_REPLACEMENTS);

                    if (!file.equals(replacedFile)) file = replacedFile;
                    else if (!name.equals(replacedName)) name = replacedName;
                }

                if (!sounds.contains(name) && sounds.contains(file)) name = file;

                instrument = Instrument.of(name);

                key += (double) (customInstrument.pitch + note.pitch) / 100;
            }

            byte layerVolume = 100;
            if (nbsLayers.size() > note.layer) {
                layerVolume = nbsLayers.get(note.layer).volume;
            }

            final double pitch = key - 33;

            song.add(
                    new Note(
                            instrument,
                            pitch,
                            key,
                            (float) note.velocity * (float) layerVolume / 10000f,
                            getMilliTime(note.tick, tempo),
                            Byte.toUnsignedInt(note.panning),
                            nbsLayers.isEmpty() ? 100 : Byte.toUnsignedInt(nbsLayers.get(note.layer).stereo),
                            isRainbowToggle
                    )
            );
        }

        song.length = song.get(song.size() - 1).time + 50;

        return song;
    }

    private static String getString (final ByteBuffer buffer, final int maxSize) throws IOException {
        final int length = buffer.getInt();
        if (length > maxSize) {
            throw new IOException("String is too large");
        }
        final byte[] arr = new byte[length];
        buffer.get(arr, 0, length);
        return new String(arr, StandardCharsets.UTF_8);
    }

    private static long getMilliTime (final long tick, final double tempo) {
        return (long) (1000L * tick * 100 / tempo);
    }

    private static final List<String> sounds = loadJsonStringArray("sounds.json");

    private static List<String> loadJsonStringArray (final String name) {
        final List<String> list = new ArrayList<>();

        final InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        assert is != null;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final JsonArray json = JsonParser.parseReader(reader).getAsJsonArray();

        for (final JsonElement entry : json) {
            list.add(entry.getAsString());
        }

        return Collections.unmodifiableList(list);
    }
}
