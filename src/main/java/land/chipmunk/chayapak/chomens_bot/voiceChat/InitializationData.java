package land.chipmunk.chayapak.chomens_bot.voiceChat;

import land.chipmunk.chayapak.chomens_bot.data.voiceChat.Codec;
import land.chipmunk.chayapak.chomens_bot.voiceChat.customPayload.SecretPacket;

import java.util.UUID;

public class InitializationData {
    public final String serverIP;
    public final int serverPort;
    public final UUID playerUUID;
    public final UUID secret;
    public final Codec codec;
    public final int mtuSize;
    public final double voiceChatDistance;
    public final int keepAlive;
    public final boolean groupsEnabled;
    public final boolean allowRecording;

    public InitializationData(String serverIP, SecretPacket secretPacket) {
        this.serverIP = serverIP;
        this.serverPort = secretPacket.serverPort;
        this.playerUUID = secretPacket.playerUUID;
        this.secret = secretPacket.secret;
        this.codec = secretPacket.codec;
        this.mtuSize = secretPacket.mtuSize;
        this.voiceChatDistance = secretPacket.voiceChatDistance;
        this.keepAlive = secretPacket.keepAlive;
        this.groupsEnabled = secretPacket.groupsEnabled;
        this.allowRecording = secretPacket.allowRecording;
    }
}
