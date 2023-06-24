package land.chipmunk.chayapak.chomens_bot.voiceChat;

import land.chipmunk.chayapak.chomens_bot.data.voiceChat.Codec;
import land.chipmunk.chayapak.chomens_bot.voiceChat.customPayload.SecretPacket;
import lombok.Getter;

import java.util.UUID;

public class InitializationData {
    @Getter private final String serverIP;
    @Getter private final int serverPort;
    @Getter private final UUID playerUUID;
    @Getter private final UUID secret;
    @Getter private final Codec codec;
    @Getter private final int mtuSize;
    @Getter private final double voiceChatDistance;
    @Getter private final int keepAlive;
    @Getter private final boolean groupsEnabled;
    @Getter private final boolean allowRecording;

    public InitializationData(String serverIP, SecretPacket secretPacket) {
        this.serverIP = serverIP;
        this.serverPort = secretPacket.serverPort();
        this.playerUUID = secretPacket.playerUUID();
        this.secret = secretPacket.secret();
        this.codec = secretPacket.codec();
        this.mtuSize = secretPacket.mtuSize();
        this.voiceChatDistance = secretPacket.voiceChatDistance();
        this.keepAlive = secretPacket.keepAlive();
        this.groupsEnabled = secretPacket.groupsEnabled();
        this.allowRecording = secretPacket.allowRecording();
    }
}
