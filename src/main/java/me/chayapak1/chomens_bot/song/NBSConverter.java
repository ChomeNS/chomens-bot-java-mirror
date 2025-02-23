package me.chayapak1.chomens_bot.song;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.LevenshteinUtilities;

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
  public static final Instrument[] instrumentIndex = new Instrument[] {
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
  public Song getSongFromBytes(byte[] bytes, String fileName, Bot bot) throws IOException {
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
    short tempo = buffer.getShort();
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
      tick += tickJumps;

      short layer = -1;
      while (true) {
        int layerJumps = buffer.getShort();
        if (layerJumps == 0) break;
        layer += layerJumps;
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
      for (int i=0; i<layerCount; i++) {
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
      Instrument instrument;
      int key = note.key;
      if (note.instrument < instrumentIndex.length) {
        instrument = instrumentIndex[note.instrument];

        key = note.key + note.pitch / 100;
      } else {
        int index = note.instrument - instrumentIndex.length;

        if (index >= customInstruments.size()) continue;

        NBSCustomInstrument customInstrument = customInstruments.get(index);

        String name = customInstrument.name.replace("entity.firework.", "entity.firework_rocket.");

        String file = Path.of(customInstrument.file).getFileName().toString();
        if (file.endsWith(".ogg")) file = file.substring(0, file.length() - ".ogg".length());
        file = file.replace("entity.firework.", "entity.firework_rocket.");

        boolean replaced = false;

        if (name.toLowerCase().contains("glass break") || name.toLowerCase().contains("glass broken")) {
          name = "block.glass.break";
          replaced = true;
        } else if (name.toLowerCase().contains("door close") || name.toLowerCase().contains("door open")) {
          name = "block.wooden_door.open";
          replaced = true;
        } else if (name.toLowerCase().contains("anvil")) {
          name = "block.anvil.fall";
          replaced = true;
        } else if (name.toLowerCase().contains("piston extend")) {
          name = "block.piston.extend";
          replaced = true;
        } else if (name.toLowerCase().contains("explosion")) {
          name = "entity.generic.explode";
          replaced = true;
        }

        if (!sounds.contains(name) && !sounds.contains(file) && !replaced) {
          name = name
                  .replaceAll("Eye.*Fill", "Eye of Ender attaches");

          final List<String> outputTitles = LevenshteinUtilities.searchTitles(name, subtitles.values());

          final String bestMatch = outputTitles.isEmpty() ? "" : outputTitles.getFirst();

          for (Map.Entry<String, String> entry : subtitles.entrySet()) {
            if (!entry.getValue().equals(bestMatch)) continue;

            name = entry.getKey().substring("subtitles.".length());

            break;
          }
        }

        if (!sounds.contains(name) && sounds.contains(file)) name = file;

        instrument = Instrument.of(name);

        key += (customInstrument.pitch + note.pitch) / 100;
      }

      byte layerVolume = 100;
      if (nbsLayers.size() > note.layer) {
        layerVolume = nbsLayers.get(note.layer).volume;
      }

      int pitch = key-33;

      try {
        song.add(new Note(instrument, pitch, key, (float) note.velocity * (float) layerVolume / 10000f, getMilliTime(note.tick, tempo), Byte.toUnsignedInt(note.panning), Byte.toUnsignedInt(nbsLayers.get(note.layer).stereo)));
      } catch (Exception e) {
        song.add(new Note(instrument, pitch, key, (float) note.velocity * (float) layerVolume / 10000f, getMilliTime(note.tick, tempo), -1, 100));
      }
    }

    song.length = song.get(song.size()-1).time + 50;

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

  private static int getMilliTime(int tick, int tempo) {
    return 1000 * tick * 100 / tempo;
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
