package land.chipmunk.chayapak.chomens_bot.voiceChat.mic;

import org.concentus.OpusApplication;
import org.concentus.OpusEncoder;

public class JavaOpusEncoder {
    public OpusEncoder opusEncoder;
    public byte[] buffer;
    public int sampleRate;
    public int frameSize;
    public de.maxhenkel.opus4j.OpusEncoder.Application application;

    public JavaOpusEncoder(int sampleRate, int frameSize, int maxPayloadSize, de.maxhenkel.opus4j.OpusEncoder.Application application) {
        this.sampleRate = sampleRate;
        this.frameSize = frameSize;
        this.application = application;
        this.buffer = new byte[maxPayloadSize];
        open();
    }

    private void open() {
        if (opusEncoder != null) {
            return;
        }
        try {
            opusEncoder = new OpusEncoder(sampleRate, 1, getApplication(application));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Opus encoder", e);
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
            throw new RuntimeException("Failed to encode audio", e);
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

    public static OpusApplication getApplication(de.maxhenkel.opus4j.OpusEncoder.Application application) {
        return switch (application) {
            default -> OpusApplication.OPUS_APPLICATION_VOIP;
            case AUDIO -> OpusApplication.OPUS_APPLICATION_AUDIO;
            case LOW_DELAY -> OpusApplication.OPUS_APPLICATION_RESTRICTED_LOWDELAY;
        };
    }
}
