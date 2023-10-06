package land.chipmunk.chayapak.chomens_bot.voiceChat.mic;

import org.concentus.OpusApplication;

public class OpusManager {
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;

    public static JavaOpusEncoder2 createEncoder(int sampleRate, int frameSize, int maxPayloadSize, OpusApplication application) {
        return new JavaOpusEncoder2(sampleRate, frameSize, maxPayloadSize, application);
    }

    public static JavaOpusEncoder2 createEncoder() {
        OpusApplication application = OpusApplication.OPUS_APPLICATION_AUDIO;

        return createEncoder(SAMPLE_RATE, FRAME_SIZE, 1024, application);
    }

    public static JavaOpusDecoder createDecoder(int sampleRate, int frameSize, int maxPayloadSize) {
        return new JavaOpusDecoder(sampleRate, frameSize, maxPayloadSize);
    }

    public static JavaOpusDecoder createDecoder() {
        return createDecoder(SAMPLE_RATE, FRAME_SIZE, 1024);
    }
}
