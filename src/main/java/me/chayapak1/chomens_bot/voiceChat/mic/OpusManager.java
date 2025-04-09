package me.chayapak1.chomens_bot.voiceChat.mic;

import org.concentus.OpusApplication;

public class OpusManager {
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;

    public static JavaOpusEncoder2 createEncoder (final int sampleRate, final int frameSize, final int maxPayloadSize, final OpusApplication application) {
        return new JavaOpusEncoder2(sampleRate, frameSize, maxPayloadSize, application);
    }

    public static JavaOpusEncoder2 createEncoder () {
        final OpusApplication application = OpusApplication.OPUS_APPLICATION_AUDIO;

        return createEncoder(SAMPLE_RATE, FRAME_SIZE, 1024, application);
    }

    public static JavaOpusDecoder createDecoder (final int sampleRate, final int frameSize, final int maxPayloadSize) {
        return new JavaOpusDecoder(sampleRate, frameSize, maxPayloadSize);
    }

    public static JavaOpusDecoder createDecoder () {
        return createDecoder(SAMPLE_RATE, FRAME_SIZE, 1024);
    }
}
