package me.chayapak1.chomens_bot.song;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.StringUtilities;
import org.cloudburstmc.math.vector.Vector3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Author: hhhzzzsss but modified quite a lot

@SuppressWarnings("unused")
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

    private record TempoSection(long startTick, double tempo) { }

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
        final double tempo = buffer.getShort();
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

        final List<NBSNote> nbsNotes = new ObjectArrayList<>();
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

        final List<NBSLayer> nbsLayers = new ObjectArrayList<>();
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

        final List<NBSCustomInstrument> customInstruments = new ObjectArrayList<>();
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

        final List<TempoSection> tempoSections = new ArrayList<>();
        tempoSections.add(new TempoSection(0, tempo)); // initial value

        if (loop > 0) {
            song.loopPosition = getMilliTime(loopStartTick, tempoSections);
            // song.loopCount = maxLoopCount;
        }

        for (final NBSNote note : nbsNotes) {
            boolean isRainbowToggle = false;
            final Instrument instrument;
            final double key;
            if (note.instrument < INSTRUMENT_INDEX.length) {
                instrument = INSTRUMENT_INDEX[note.instrument];

                key = (double) ((note.key * 100) + note.pitch) / 100;
            } else {
                final int index = note.instrument - INSTRUMENT_INDEX.length;

                if (index >= customInstruments.size()) continue;

                final NBSCustomInstrument customInstrument = customInstruments.get(index);

                String name = customInstrument.name
                        // firework has been replaced with firework_rocket in newer minecraft versions
                        .replace("entity.firework.", "entity.firework_rocket.");

                boolean isTempoChanger = false;

                if (name.equals("Tempo Changer")) {
                    isTempoChanger = true;

                    tempoSections.add(
                            new TempoSection(
                                    note.tick,
                                    (double) Math.abs(note.pitch) * 100 / 15
                            )
                    );
                } else if (name.equals("Toggle Rainbow")) {
                    isRainbowToggle = true;
                }

                final String file = customInstrument.file
                        .replaceFirst("minecraft/|Custom/", "")
                        .replace(".ogg", "");

                if (!isTempoChanger && !isRainbowToggle) {
                    if (!playSound.contains(name) && minecraftToPlaySound.containsKey(file)) name = minecraftToPlaySound.get(file);
                    else if (playSound.contains(file) && !minecraftToPlaySound.containsKey(name)) name = file;
                }

                instrument = Instrument.of(name);

                key = note.key + customInstrument.pitch - 45 + ((double) note.pitch / 100);
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
                            getMilliTime(note.tick, tempoSections),
                            getPosition(
                                    Byte.toUnsignedInt(note.panning),
                                    nbsLayers.isEmpty() ? 100 : Byte.toUnsignedInt(nbsLayers.get(note.layer).stereo)
                            ),
                            isRainbowToggle
                    )
            );
        }

        song.length = song.get(song.size() - 1).time + 50;

        return song;
    }

    private Vector3d getPosition (final int panning, final int stereo) {
        final double value;

        if (stereo == 100 && panning != 100) value = panning;
        else if (panning == 100 && stereo != 100) value = stereo;
        else value = (double) (stereo + panning) / 2;

        final double xPos;

        if (value > 100) xPos = (value - 100) / -100;
        else if (value == 100) xPos = 0;
        else xPos = ((value - 100) * -1) / 100;

        return Vector3d.from(xPos * 2, 0, 0);
    }

    private static String getString (final ByteBuffer buffer, final int maxSize) throws IOException {
        final int length = buffer.getInt();
        if (length > maxSize) {
            throw new IOException("String is too large");
        }
        final byte[] arr = new byte[length];
        buffer.get(arr, 0, length);
        return StringUtilities.fromUTF8Lossy(arr);
    }

    // Author: ChatGPT (lmao, but it actually works tho, I don't even know how it worked)
    private static long getMilliTime (final long currentTick, final List<TempoSection> sections) {
        long totalMillis = 0;

        for (int i = 0; i < sections.size(); i++) {
            final TempoSection current = sections.get(i);
            final TempoSection next = (i + 1 < sections.size()) ? sections.get(i + 1) : null;

            final long startTick = current.startTick;
            final long endTick = (next != null) ? Math.min(next.startTick, currentTick) : currentTick;

            if (currentTick < startTick) break;

            final long ticksInThisSegment = endTick - startTick;
            totalMillis += (long) (1000L * ticksInThisSegment * 100 / current.tempo);
        }

        return totalMillis;
    }

    private static final Map<String, String> minecraftToPlaySound = new Object2ObjectOpenHashMap<>();
    private static final List<String> playSound = new ObjectArrayList<>();

    static {
        final InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("sounds.json");
        assert is != null;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            final String playSoundName = entry.getKey();
            final JsonObject data = entry.getValue().getAsJsonObject();
            final JsonArray sounds = data.getAsJsonArray("sounds");

            for (final JsonElement element : sounds) {
                final String sound;
                if (element.isJsonObject()) {
                    final JsonObject object = element.getAsJsonObject();
                    sound = object.get("name").getAsString();
                } else {
                    sound = element.getAsString();
                }
                minecraftToPlaySound.put(sound, playSoundName);
                playSound.add(playSoundName);
            }
        }
    }
}
