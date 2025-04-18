package me.chayapak1.chomens_bot.voiceChat.mic;

import org.concentus.OpusDecoder;
import org.concentus.OpusException;

public class JavaOpusDecoder {
    protected OpusDecoder opusDecoder;
    protected final short[] buffer;
    protected final int sampleRate;
    protected final int frameSize;
    protected final int maxPayloadSize;

    public JavaOpusDecoder (final int sampleRate, final int frameSize, final int maxPayloadSize) {
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        this.maxPayloadSize = maxPayloadSize;
        this.buffer = new short[4096];
        open();
    }

    private void open () {
        if (opusDecoder != null) {
            return;
        }
        try {
            opusDecoder = new OpusDecoder(sampleRate, 1);
        } catch (final OpusException e) {
            throw new IllegalStateException("Opus decoder error " + e.getMessage());
        }
    }

    public short[] decode (final byte[] data) {
        if (isClosed()) {
            throw new IllegalStateException("Decoder is closed");
        }
        final int result;

        try {
            if (data == null || data.length == 0) {
                result = opusDecoder.decode(null, 0, 0, buffer, 0, frameSize, false);
            } else {
                result = opusDecoder.decode(data, 0, data.length, buffer, 0, frameSize, false);
            }
        } catch (final Exception e) {
            throw new RuntimeException("Failed to decode audio data: " + e.getMessage());
        }

        final short[] audio = new short[result];
        System.arraycopy(buffer, 0, audio, 0, result);
        return audio;
    }

    public boolean isClosed () {
        return opusDecoder == null;
    }

    public void close () {
        if (opusDecoder == null) {
            return;
        }
        opusDecoder = null;
    }

    public void resetState () {
        if (isClosed()) {
            throw new IllegalStateException("Decoder is closed");
        }
        opusDecoder.resetState();
    }
}
