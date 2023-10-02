package land.chipmunk.chayapak.chomens_bot.song;

import land.chipmunk.chayapak.chomens_bot.Bot;

import javax.sound.midi.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static javax.sound.midi.ShortMessage.SYSTEM_RESET;

// Author: hhhzzzsss
// i modified this and some codes are stolen from https://github.com/RaphiMC/NoteBlockLib so thanks to them uwu
public class MidiConverter implements Converter {
  public static final int TEXT = 0x01;
  public static final int TRACK_NAME = 0x03;
  public static final int LYRICS = 0x05;
  public static final int VOLUME_CONTROL_MSB = 0x07;
  public static final int PAN_CONTROL_MSB = 0x0A;
  public static final int SET_INSTRUMENT = 0xC0;
  public static final int SET_TEMPO = 0x51;
  public static final int RESET_CONTROLS = 0x79;

  @Override
  public Song getSongFromBytes(byte[] bytes, String name, Bot bot) throws InvalidMidiDataException, IOException {
    Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(bytes));
    return getSong(sequence, name, bot);
  }
  
  public static Song getSong(Sequence sequence, String name, Bot bot) {
    final Map<Long, String> lyrics = new HashMap<>();

    long tpq = sequence.getResolution();

    String songName = null;

    final StringBuilder text = new StringBuilder();

    boolean isFirst = true;

    ArrayList<MidiEvent> tempoEvents = new ArrayList<>();
    for (Track track : sequence.getTracks()) {
      final int trackSize = track.size();

      for (int i = 0; i < trackSize; i++) {
        MidiEvent event = track.get(i);
        MidiMessage message = event.getMessage();
        if (message instanceof MetaMessage mm) {
          if (mm.getType() == SET_TEMPO) {
            tempoEvents.add(event);
          } else if (mm.getType() == TRACK_NAME && isFirst) {
            final String stringTitle = new String(mm.getData());

            if (!stringTitle.isBlank()) {
              songName = stringTitle + " (" + name + ")"; // i have put the ` (filename)` just in case the sequence is getting sus (like Track 2 for example)

              isFirst = false;
            }
          } else if (mm.getType() == TEXT) {
            text.append(new String(mm.getData()));
            text.append('\n');
          } else if (mm.getType() == LYRICS) {
            final String lyric = new String(mm.getMessage());

            lyrics.put(event.getTick(), lyric);
          }
        }
      }
    }

    String stringText = text.toString();
    if (stringText.endsWith("\n")) stringText = stringText.substring(0, stringText.length() - 1);

    final Song song = new Song(name, bot, songName, null, null, stringText, false);
    
    tempoEvents.sort(Comparator.comparingLong(MidiEvent::getTick));

    final int[] channelVolumes = new int[16];
    final int[] channelPans = new int[16];
    Arrays.fill(channelVolumes, 127);
    Arrays.fill(channelPans, 64);
    
    for (Track track : sequence.getTracks()) {
      long microTime = 0;
      int[] ids = new int[16];
      int mpq = 500000;
      int tempoEventIdx = 0;
      long prevTick = 0;

      final int trackSize = track.size();

      for (int i = 0; i < trackSize; i++) {
        MidiEvent event = track.get(i);
        MidiMessage message = event.getMessage();
        
        while (tempoEventIdx < tempoEvents.size() && event.getTick() > tempoEvents.get(tempoEventIdx).getTick()) {
          long deltaTick = tempoEvents.get(tempoEventIdx).getTick() - prevTick;
          prevTick = tempoEvents.get(tempoEventIdx).getTick();
          microTime += (mpq/tpq) * deltaTick;
          
          MetaMessage mm = (MetaMessage) tempoEvents.get(tempoEventIdx).getMessage();
          byte[] data = mm.getData();
          int new_mpq = (data[2]&0xFF) | ((data[1]&0xFF)<<8) | ((data[0]&0xFF)<<16);
          if (new_mpq != 0) mpq = new_mpq;
          tempoEventIdx++;
        }
        
        if (message instanceof ShortMessage sm) {
          if (sm.getCommand() == SET_INSTRUMENT) {
            ids[sm.getChannel()] = sm.getData1();
          }
          else if (sm.getCommand() == ShortMessage.NOTE_ON) {
            if (sm.getData2() == 0) continue;
            int pitch = sm.getData1();
            int velocity = sm.getData2();
            int effectiveVelocity = (int) ((float) velocity * channelVolumes[sm.getChannel()] / 127);
            int pan = channelPans[sm.getChannel()];
            long deltaTick = event.getTick() - prevTick;
            prevTick = event.getTick();
            microTime += (mpq/tpq) * deltaTick;

            Note note;
            if (sm.getChannel() == 9) {
              note = getMidiPercussionNote(pitch, effectiveVelocity, microTime, pan);
            }
            else {
              note = getMidiInstrumentNote(ids[sm.getChannel()], pitch, effectiveVelocity, microTime, pan);
            }
            if (note != null) {
              song.add(note);
            }

            long time = microTime / 1000L;
            if (time > song.length) {
              song.length = time;
            }
          }
          else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
            long deltaTick = event.getTick() - prevTick;
            prevTick = event.getTick();
            microTime += (mpq/tpq) * deltaTick;
            long time = microTime / 1000L;
            if (time > song.length) {
              song.length = time;
            }
          }
          else if (sm.getCommand() == ShortMessage.CONTROL_CHANGE) {
            if (sm.getData1() == VOLUME_CONTROL_MSB) {
              channelVolumes[sm.getChannel()] = sm.getData2();
            } else if (sm.getData1() == PAN_CONTROL_MSB) {
              channelPans[sm.getChannel()] = sm.getData2();
            } else if (sm.getData1() == RESET_CONTROLS) {
              channelVolumes[sm.getChannel()] = 127;
              channelPans[sm.getChannel()] = 64;
            }
          }
          else if (sm.getCommand() == SYSTEM_RESET) {
            Arrays.fill(channelVolumes, 127);
            Arrays.fill(channelPans, 64);
          }
        }

        if (lyrics.get(event.getTick()) != null) {
          song.lyrics.put(microTime / 1000L, lyrics.get(event.getTick()));
        }
      }
    }

    song.sort();
    
    return song;
  }

  public static Note getMidiInstrumentNote(int midiInstrument, int midiPitch, int velocity, long microTime, int panning) {
    Instrument instrument = null;
    Instrument[] instrumentList = instrumentMap.get(midiInstrument);
    if (instrumentList != null) {
      for (Instrument candidateInstrument : instrumentList) {
        if (midiPitch >= candidateInstrument.offset && midiPitch <= candidateInstrument.offset+24) {
          instrument = candidateInstrument;
          break;
        }
      }

      if (instrument == null) {
//        instrument = instrumentList[0];

        // we are finding the closest instrument offset here and use that
        // closest instrument as the instrument

        final Integer[] offsets = Arrays.stream(instrumentList).map(ins -> ins.offset).toArray(Integer[]::new);

        // https://stackoverflow.com/questions/13318733/get-closest-value-to-a-number-in-array
        int distance = Math.abs(offsets[0] - midiPitch);
        int idx = 0;
        for (int c = 1; c < offsets.length; c++) {
          int cdistance = Math.abs(offsets[c] - midiPitch);
          if (cdistance < distance) {
            idx = c;
            distance = cdistance;
          }
        }

        final int closest = offsets[idx];

        instrument = Arrays.stream(instrumentList)
                .filter(ins -> ins.offset == closest)
                .toArray(Instrument[]::new)[0];
      }
    }

    if (instrument == null) {
      return null;
    }

    int pitch = midiPitch-instrument.offset;

    float volume = (float) velocity / 127.0f;
    long time = microTime / 1000L;

    return new Note(instrument, pitch, midiPitch, volume, time, (int) ((panning - 64) / (float) 64), 100);
  }

  private static Note getMidiPercussionNote (int midiPitch, int velocity, long microTime, int panning) {
    if (percussionMap.containsKey(midiPitch)) {
      int noteId = percussionMap.get(midiPitch);
      int pitch = noteId % 25;
      float volume = (float) velocity / 127.0f;
      Instrument instrument = Instrument.fromId(noteId / 25);
      long time = microTime / 1000L;

      return new Note(instrument, pitch, midiPitch, volume, time, (int) ((panning - 64) / (float) 64), 100);
    }
    return null;
  }

  public static final HashMap<Integer, Instrument[]> instrumentMap = new HashMap<>();
  static {
    // Piano (HARP BASS BELL)
    instrumentMap.put(0, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Acoustic Grand Piano
    instrumentMap.put(1, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Bright Acoustic Piano
    instrumentMap.put(2, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Electric Grand Piano
    instrumentMap.put(3, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Honky-tonk Piano
    instrumentMap.put(4, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Electric Piano 1
    instrumentMap.put(5, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Electric Piano 2
    instrumentMap.put(6, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Harpsichord
    instrumentMap.put(7, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Clavinet

    // Chromatic Percussion (IRON_XYLOPHONE XYLOPHONE BASS)
    instrumentMap.put(8, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Celesta
    instrumentMap.put(9, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Glockenspiel
    instrumentMap.put(10, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Music Box
    instrumentMap.put(11, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Vibraphone
    instrumentMap.put(12, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Marimba
    instrumentMap.put(13, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Xylophone
    instrumentMap.put(14, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Tubular Bells
    instrumentMap.put(15, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE}); // Dulcimer

    // Organ (BIT DIDGERIDOO BELL)
    instrumentMap.put(16, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Drawbar Organ
    instrumentMap.put(17, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Percussive Organ
    instrumentMap.put(18, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Rock Organ
    instrumentMap.put(19, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Church Organ
    instrumentMap.put(20, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Reed Organ
    instrumentMap.put(21, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Accordian
    instrumentMap.put(22, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Harmonica
    instrumentMap.put(23, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Tango Accordian

    // Guitar (BIT DIDGERIDOO BELL)
    instrumentMap.put(24, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Acoustic Guitar (nylon)
    instrumentMap.put(25, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Acoustic Guitar (steel)
    instrumentMap.put(26, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Electric Guitar (jazz)
    instrumentMap.put(27, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Electric Guitar (clean)
    instrumentMap.put(28, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Electric Guitar (muted)
    instrumentMap.put(29, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Overdriven Guitar
    instrumentMap.put(30, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Distortion Guitar
    instrumentMap.put(31, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Guitar Harmonics

    // Bass
    instrumentMap.put(32, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Acoustic Bass
    instrumentMap.put(33, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Electric Bass (finger)
    instrumentMap.put(34, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Electric Bass (pick)
    instrumentMap.put(35, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Fretless Bass
    instrumentMap.put(36, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Slap Bass 1
    instrumentMap.put(37, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Slap Bass 2
    instrumentMap.put(38, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Synth Bass 1
    instrumentMap.put(39, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Synth Bass 2

    // Strings
    instrumentMap.put(40, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Violin
    instrumentMap.put(41, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Viola
    instrumentMap.put(42, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Cello
    instrumentMap.put(43, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Contrabass
    instrumentMap.put(44, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Tremolo Strings
    instrumentMap.put(45, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Pizzicato Strings
    instrumentMap.put(46, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.CHIME}); // Orchestral Harp
    instrumentMap.put(47, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Timpani

    // Ensenble
    instrumentMap.put(48, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // String Ensemble 1
    instrumentMap.put(49, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // String Ensemble 2
    instrumentMap.put(50, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Synth Strings 1
    instrumentMap.put(51, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Synth Strings 2
    instrumentMap.put(52, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Choir Aahs
    instrumentMap.put(53, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Voice Oohs
    instrumentMap.put(54, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Synth Choir
    instrumentMap.put(55, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Orchestra Hit

    // Brass
    instrumentMap.put(56, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(57, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(58, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(59, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(60, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(61, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(62, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(63, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});

    // Reed
    instrumentMap.put(64, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(65, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(66, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(67, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(68, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(69, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(70, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(71, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});

    // Pipe
    instrumentMap.put(72, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(73, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(74, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(75, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(76, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(77, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(78, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
    instrumentMap.put(79, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});

    // Synth Lead
    instrumentMap.put(80, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(81, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(82, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(83, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(84, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(85, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(86, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(87, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});

    // Synth Pad
    instrumentMap.put(88, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(89, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(90, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(91, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(92, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(93, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(94, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(95, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});

    // Synth Effects
//    instrumentMap.put(96, new Instrument[]{});
//    instrumentMap.put(97, new Instrument[]{});
    instrumentMap.put(98, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(99, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(100, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(101, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(102, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(103, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});

    // Ethnic
    instrumentMap.put(104, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(105, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(106, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(107, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(108, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
    instrumentMap.put(109, new Instrument[]{Instrument.HARP, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(110, new Instrument[]{Instrument.HARP, Instrument.DIDGERIDOO, Instrument.BELL});
    instrumentMap.put(111, new Instrument[]{Instrument.HARP, Instrument.DIDGERIDOO, Instrument.BELL});

    // Percussive
    instrumentMap.put(112, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
    instrumentMap.put(113, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
    instrumentMap.put(114, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
    instrumentMap.put(115, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
    instrumentMap.put(116, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
    instrumentMap.put(117, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
    instrumentMap.put(118, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
    instrumentMap.put(119, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
  }

  public static final HashMap<Integer, Integer> percussionMap = new HashMap<>();
  static {
    percussionMap.put(35, 10 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(36, 6  + 25*Instrument.BASEDRUM.id);
    percussionMap.put(37, 6  + 25*Instrument.HAT.id);
    percussionMap.put(38, 8  + 25*Instrument.SNARE.id);
    percussionMap.put(39, 6  + 25*Instrument.HAT.id);
    percussionMap.put(40, 4  + 25*Instrument.SNARE.id);
    percussionMap.put(41, 6  + 25*Instrument.BASEDRUM.id);
    percussionMap.put(42, 22 + 25*Instrument.SNARE.id);
    percussionMap.put(43, 13 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(44, 22 + 25*Instrument.SNARE.id);
    percussionMap.put(45, 15 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(46, 18 + 25*Instrument.SNARE.id);
    percussionMap.put(47, 20 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(48, 23 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(49, 17 + 25*Instrument.SNARE.id);
    percussionMap.put(50, 23 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(51, 24 + 25*Instrument.SNARE.id);
    percussionMap.put(52, 8  + 25*Instrument.SNARE.id);
    percussionMap.put(53, 13 + 25*Instrument.SNARE.id);
    percussionMap.put(54, 18 + 25*Instrument.HAT.id);
    percussionMap.put(55, 18 + 25*Instrument.SNARE.id);
    percussionMap.put(56, 1  + 25*Instrument.HAT.id);
    percussionMap.put(57, 13 + 25*Instrument.SNARE.id);
    percussionMap.put(58, 2  + 25*Instrument.HAT.id);
    percussionMap.put(59, 13 + 25*Instrument.SNARE.id);
    percussionMap.put(60, 9  + 25*Instrument.HAT.id);
    percussionMap.put(61, 2  + 25*Instrument.HAT.id);
    percussionMap.put(62, 8  + 25*Instrument.HAT.id);
    percussionMap.put(63, 22 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(64, 15 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(65, 13 + 25*Instrument.SNARE.id);
    percussionMap.put(66, 8  + 25*Instrument.SNARE.id);
    percussionMap.put(67, 8  + 25*Instrument.HAT.id);
    percussionMap.put(68, 3  + 25*Instrument.HAT.id);
    percussionMap.put(69, 20 + 25*Instrument.HAT.id);
    percussionMap.put(70, 23 + 25*Instrument.HAT.id);
    percussionMap.put(71, 24 + 25*Instrument.HAT.id);
    percussionMap.put(72, 24 + 25*Instrument.HAT.id);
    percussionMap.put(73, 17 + 25*Instrument.HAT.id);
    percussionMap.put(74, 11 + 25*Instrument.HAT.id);
    percussionMap.put(75, 18 + 25*Instrument.HAT.id);
    percussionMap.put(76, 9  + 25*Instrument.HAT.id);
    percussionMap.put(77, 5  + 25*Instrument.HAT.id);
    percussionMap.put(78, 22 + 25*Instrument.HAT.id);
    percussionMap.put(79, 19 + 25*Instrument.SNARE.id);
    percussionMap.put(80, 17 + 25*Instrument.HAT.id);
    percussionMap.put(81, 22 + 25*Instrument.HAT.id);
    percussionMap.put(82, 22 + 25*Instrument.SNARE.id);
    percussionMap.put(83, 24 + 25*Instrument.CHIME.id);
    percussionMap.put(84, 24 + 25*Instrument.CHIME.id);
    percussionMap.put(85, 21 + 25*Instrument.HAT.id);
    percussionMap.put(86, 14 + 25*Instrument.BASEDRUM.id);
    percussionMap.put(87, 7  + 25*Instrument.BASEDRUM.id);
  }
}
