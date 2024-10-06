package me.chayapak1.chomens_bot.voiceChat.mic;

public class MicThread extends Thread {
    // this will probably never be finished
//    private Microphone mic;
//    private boolean running;
//    private boolean microphoneLocked;
//    private boolean wasWhispering;
//    private final JavaOpusEncoder2 encoder;
//
//    public MicThread() {
//        this.running = true;
//        this.encoder = OpusManager.createEncoder();
//
//        setDaemon(true);
//        setName("Simple Voice Chat Microphone Thread");
//    }
//
//    @Override
//    public void run() {
//        Microphone mic = getMic();
//        if (mic == null) {
//            return;
//        }
//
//        while (running) {
//            short[] audio = pollMic();
//            if (audio == null) {
//                continue;
//            }
//
//            voice(audio);
//        }
//    }
//
//    public short[] pollMic() {
//        Microphone mic = getMic();
//        if (mic == null) {
//            throw new IllegalStateException("No microphone available");
//        }
//        if (!mic.isStarted()) {
//            mic.start();
//        }
//
//        if (mic.available() < SoundManager.FRAME_SIZE) {
//            Utils.sleep(5);
//            return null;
//        }
//        short[] buff = mic.read();
//        volumeManager.adjustVolumeMono(buff, VoicechatClient.CLIENT_CONFIG.microphoneAmplification.get().floatValue());
//        return denoiseIfEnabled(buff);
//    }
//
//    private Microphone getMic() {
//        if (!running) {
//            return null;
//        }
//        if (mic == null) {
//            try {
//                mic = MicrophoneManager.createMicrophone();
//            } catch (MicrophoneException e) {
//                running = false;
//                return null;
//            }
//        }
//        return mic;
//    }
//
//    private volatile boolean activating;
//    private volatile int deactivationDelay;
//    private volatile short[] lastBuff;
//
//    private void voice(short[] audio) {
//        sendAudioPacket(audio);
//        lastBuff = audio;
//    }
//
//    private void flush() {
//        sendStopPacket();
//        if (!encoder.isClosed()) {
//            encoder.resetState();
//        }
//    }
//
//    public boolean isTalking() {
//        return !microphoneLocked && (activating || wasPTT);
//    }
//
//    public boolean isWhispering() {
//        return isTalking() && wasWhispering;
//    }
//
//    public void close() {
//        if (!running) {
//            return;
//        }
//        running = false;
//
//        if (Thread.currentThread() != this) {
//            try {
//                join(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        mic.close();
//        encoder.close();
//        flush();
//    }
//
//    private final AtomicLong sequenceNumber = new AtomicLong();
//    private volatile boolean stopPacketSent = true;
//
//    private void sendAudioPacket(short[] data) {
//        short[] audio = PluginManager.instance().onClientSound(data, whispering);
//        if (audio == null) {
//            return;
//        }
//
//        try {
//            if (connection != null && connection.isInitialized()) {
//                byte[] encoded = encoder.encode(audio);
//                connection.sendToServer(new NetworkMessage(new MicPacket(encoded, whispering, sequenceNumber.getAndIncrement())));
//                stopPacketSent = false;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            if (client != null && client.getRecorder() != null) {
//                client.getRecorder().appendChunk(Minecraft.getInstance().getUser().getGameProfile().getId(), System.currentTimeMillis(), PositionalAudioUtils.convertToStereo(audio));
//            }
//        } catch (IOException e) {
//            Voicechat.LOGGER.error("Failed to record audio", e);
//            client.setRecording(false);
//        }
//    }
//
//    private void sendStopPacket() {
//        if (stopPacketSent) {
//            return;
//        }
//
//        if (connection == null || !connection.isInitialized()) {
//            return;
//        }
//        try {
//            connection.sendToServer(new NetworkMessage(new MicPacket(new byte[0], false, sequenceNumber.getAndIncrement())));
//            stopPacketSent = true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
