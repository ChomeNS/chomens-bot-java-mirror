package me.chayapak1.chomens_bot.voiceChat.mic;

import org.concentus.OpusApplication;
import org.concentus.OpusEncoder;

public class JavaOpusEncoder2 {
    public OpusEncoder opusEncoder;
    public byte[] buffer;
    public int sampleRate;
    public int frameSize;
    public int maxPayloadSize;
    public OpusApplication application;

    public JavaOpusEncoder2 (int sampleRate, int frameSize, int maxPayloadSize, OpusApplication application) {
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        this.maxPayloadSize = maxPayloadSize;
        this.application = application;
        this.buffer = new byte[maxPayloadSize];
        open();
    }

    private void open() {
        if (opusEncoder != null) {
            return;
        }
        try {
            opusEncoder = new OpusEncoder(sampleRate, 1, application);
        } catch (Exception e) {
            throw new IllegalStateException("Opus encoder error " + e.getMessage());
        }
    }

    public byte[] encode(short[] rawAudio) {
        if (isClosed()) {
            throw new IllegalStateException("Encoder is closed");
        }

        int result;
        try {
            result = opusEncoder.encode(rawAudio, 0, frameSize, buffer, 0, buffer.length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode audio data: " + e.getMessage());
        }

        if (result < 0) {
            throw new RuntimeException("Failed to encode audio data");
        }

        byte[] audio = new byte[result];
        System.arraycopy(buffer, 0, audio, 0, result);
        return audio;
    }

    public void resetState() {
        if (isClosed()) {
            throw new IllegalStateException("Encoder is closed");
        }
        opusEncoder.resetState();
    }

    public boolean isClosed() {
        return opusEncoder == null;
    }

    public void close() {
        if (isClosed()) {
            return;
        }
        opusEncoder = null;
    }
}
